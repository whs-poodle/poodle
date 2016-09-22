/*
 * Copyright 2015 Westfälische Hochschule
 *
 * This file is part of Poodle.
 *
 * Poodle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Poodle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Poodle.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.whs.poodle.repositories;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.whs.poodle.beans.ExerciseWorksheet;
import de.whs.poodle.beans.forms.FeedbackForm;
import de.whs.poodle.beans.statistics.Statistic.StatisticSource;

@Repository
public class FeedbackRepository {

	@Autowired
	private StudentToCourseTermRepository studentToCourseTermRepo;

	@Autowired
	private JdbcTemplate jdbc;

	@Transactional
	public void saveFeedbackAndRemoveExerciseFromSelfStudy(
			FeedbackForm form,
			int studentId,
			int courseTermId) {
		saveFeedback(form, studentId, courseTermId, StatisticSource.SELF_STUDY);
		studentToCourseTermRepo.removeExerciseFromWorksheet(studentId, courseTermId, form.getExerciseId());
	}

	public void saveFeedback(
			FeedbackForm form,
			int studentId,
			int courseTermId,
			StatisticSource source) {

		// make sure we don't insert an empty string into the database
		String text = form.getText();
		if (text != null && text.trim().isEmpty())
			text = null;

		jdbc.update(
			"INSERT INTO statistic" +
			"(difficulty,fun,text,time,completion_status,source,course_term_id,exercise_id,student_id) " +
			"VALUES(" +
				"?,?,?,?,?::completion_status,?::statistic_source,?,?,?" +
			")",
			new Object[] {
				form.getDifficulty(), form.getFun(),
				text, form.getTime(),
				form.getStatus(), source,
				courseTermId, form.getExerciseId(),
				studentId
			},
			/* we have to specify they types because it can't figure
			 * out the types for null values otherwise */
			new int[]{
				Types.INTEGER, Types.INTEGER,
				Types.VARCHAR, Types.INTEGER,
				Types.VARCHAR, Types.VARCHAR,
				Types.INTEGER, Types.INTEGER,
				Types.INTEGER
			});
	}

	/*
	 * returns a map defining whether the student has completed each worksheet (i.e. all exercises on a worksheet).
	 */
	public HashMap<ExerciseWorksheet,Boolean> getExerciseWorksheetIsCompletedMap(int studentId, List<ExerciseWorksheet> worksheets) {
		HashMap<ExerciseWorksheet,Boolean> map = new HashMap<>();

		for (ExerciseWorksheet ab : worksheets) {
			// check for each exercise on the worksheet if a matching entry in statistics exists
			boolean hasFeedback = jdbc.queryForObject(
					"SELECT NOT EXISTS " +
					"(SELECT * FROM chapter_to_exercise ka " +
					"  JOIN chapter k ON ka.chapter_id = k.id " +
					"  JOIN exercise a ON ka.exercise_id = a.id " +
					"  WHERE k.worksheet_id = ? " +
					"  AND NOT EXISTS (" +
					"	 SELECT * FROM v_statistic s" +
					"	 WHERE s.student_id = ? " +
					"	 AND a.root_id = s.exercise_root_id " +
					"  )" +
					") AS hasFeedback",

					new Object[]{ab.getId(), studentId}, Boolean.class);

			map.put(ab, hasFeedback);
		}

		return map;
	}
}
