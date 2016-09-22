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
package de.whs.poodle.services;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import de.whs.poodle.beans.Chapter;
import de.whs.poodle.beans.Chapter.ExerciseInChapter;
import de.whs.poodle.beans.ExerciseWorksheet;
import de.whs.poodle.beans.Worksheet.WorksheetType;
import de.whs.poodle.beans.evaluation.EvaluationQuestion;
import de.whs.poodle.beans.evaluation.EvaluationSection;
import de.whs.poodle.beans.evaluation.EvaluationWorksheet;
import de.whs.poodle.beans.mc.InstructorMcWorksheet;
import de.whs.poodle.beans.mc.McQuestionOnWorksheet;
import de.whs.poodle.repositories.EvaluationWorksheetRepository;
import de.whs.poodle.repositories.ExerciseWorksheetRepository;
import de.whs.poodle.repositories.InstructorMcWorksheetRepository;
import de.whs.poodle.repositories.WorksheetRepository;
import de.whs.poodle.repositories.exceptions.BadRequestException;

/*
 * Service to add a term to a course and copy the worksheets from the
 * previous courseTerm to the new one. This is a separate service to avoid
 * circular dependencies between the Repository beans.
 */
@Service
public class AddTermToCourseService {

	@Autowired
	private WorksheetRepository worksheetRepo;

	@Autowired
	private ExerciseWorksheetRepository exerciseWorksheetRepo;

	@Autowired
	private InstructorMcWorksheetRepository instructorMcWorksheetRepo;

	@Autowired
	private EvaluationWorksheetRepository evaluationWorksheetRepo;

	@Autowired
	private JdbcTemplate jdbc;

	@Transactional
	public void addTermToCourse(int instructorId, int courseId, String termName) {
		// create the new courseTerm
		int newCourseTermId;
		try {
			newCourseTermId = jdbc.queryForObject(
					"INSERT INTO course_term(course_id,term) VALUES(?,?) RETURNING id",
					new Object[]{courseId, termName},
					Integer.class);
		} catch(DuplicateKeyException e) {
			// this term already exists for this course
			throw new BadRequestException();
		}

		// get ID of the previous courseTerm so we can copy over the worksheets
		int prevCourseTermId = jdbc.queryForObject(
				"SELECT id FROM v_course_term " +
				"WHERE course_id = ? AND NOT is_latest ORDER BY id DESC LIMIT 1",
				new Object[]{courseId},
				Integer.class);

		copyWorksheets(prevCourseTermId, newCourseTermId);
	}

	@Transactional
	private void copyWorksheets(int fromCourseTermId, int toCourseTermId) {
		// copy exercise worksheets
		List<ExerciseWorksheet> exerciseWorksheets = exerciseWorksheetRepo.getForCourseTerm(fromCourseTermId);

		for (ExerciseWorksheet w : exerciseWorksheets) {
			int worksheetCopyId = worksheetRepo.create(w.getTitle(), toCourseTermId, WorksheetType.EXERCISE);

			for (Chapter c : w.getChapters()) {
				int chapterCopyId = exerciseWorksheetRepo.addChapter(worksheetCopyId, c.getTitle());

				for (ExerciseInChapter e : c.getExercises()) {
					exerciseWorksheetRepo.addExerciseToChapter(chapterCopyId, e.getExercise().getId());
				}
			}
		}

		// copy mc worksheets
		List<InstructorMcWorksheet> mcWorksheets = instructorMcWorksheetRepo.getForCourseTerm(fromCourseTermId);

		for (InstructorMcWorksheet w : mcWorksheets) {
			/* note that create() returns the ID of the Worksheet. However, we need the ID
			 * of the McWorksheet in order to add the questions to the copy. */
			int worksheetCopyId = worksheetRepo.create(w.getTitle(), toCourseTermId, WorksheetType.MC);
			InstructorMcWorksheet worksheetCopy = instructorMcWorksheetRepo.getById(worksheetCopyId);
			int mcWorksheetCopyId = worksheetCopy.getMcWorksheetId();

			for (McQuestionOnWorksheet q : w.getQuestions()) {
				instructorMcWorksheetRepo.addQuestion(mcWorksheetCopyId, q.getQuestion().getId());
			}
		}

		// evaluation
		EvaluationWorksheet evaluation = evaluationWorksheetRepo.getForCourseTerm(fromCourseTermId);
		if (evaluation != null) {
			// create a worksheet in the new course term
			int evaluationCopyId = evaluationWorksheetRepo.create(toCourseTermId);

			for (EvaluationSection section : evaluation.getSections()) {
				int sectionCopyId = evaluationWorksheetRepo.addSection(evaluationCopyId, section.getTitle(), section.getNumber());

				for (EvaluationQuestion question : section.getQuestions()) {
					evaluationWorksheetRepo.addQuestionToSection(sectionCopyId, question);
				}
			}
		}
	}
}
