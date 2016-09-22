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

import java.sql.CallableStatement;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import de.whs.poodle.beans.ExerciseWorksheet;
import de.whs.poodle.repositories.exceptions.BadRequestException;
import de.whs.poodle.repositories.exceptions.NotFoundException;

/*
 * Contains all functions that are specific to ExerciseWorksheet. Note
 * that a lot of functions shared between both Worksheet types are
 * in WorksheetRepository.
 */
@Repository
public class ExerciseWorksheetRepository {

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;


	public List<ExerciseWorksheet> getUnlockedForCourseTerm(int courseTermId) {
		return em.createQuery(
				"FROM ExerciseWorksheet ws " +
				"WHERE ws.courseTerm.id = :courseTermId " +
				"AND unlocked = TRUE " +
				"ORDER BY ws.number", ExerciseWorksheet.class)
				.setParameter("courseTermId", courseTermId)
				.getResultList();
	}

	public List<ExerciseWorksheet> getForCourseTerm(int courseTermId) {
		return em.createQuery(
				"FROM ExerciseWorksheet ws " +
				"WHERE ws.courseTerm.id = :courseTermId " +
				"ORDER BY ws.number", ExerciseWorksheet.class)
				.setParameter("courseTermId", courseTermId)
				.getResultList();
	}

	public ExerciseWorksheet getById(int id) {
		ExerciseWorksheet ws = em.find(ExerciseWorksheet.class, id);
		if (ws == null)
			throw new NotFoundException();

		return ws;
	}


	public void removeChapter(int chapterId) {
		jdbc.update(
				con -> {
					CallableStatement cs = con.prepareCall("{ CALL remove_chapter(?) }");
					cs.setInt(1, chapterId);
					return cs;
				}
		);
	}

	public void renameChapter(int chapterId, String newTitle) {
		if (newTitle == null || newTitle.trim().isEmpty())
			throw new BadRequestException();

		jdbc.update("UPDATE chapter SET title = ? WHERE id = ?", newTitle, chapterId);
	}

	public void moveChapter(int chapterId, boolean up) {
		jdbc.update(
				con -> {
					CallableStatement cs = con.prepareCall("{ CALL move_chapter(?,?) }");
					cs.setInt(1, chapterId);
					cs.setBoolean(2, up);
					return cs;
				}
		);
	}


	public void moveExercise(int chapterId, int exerciseId, boolean up) {
		jdbc.update(
				con -> {
					CallableStatement cs = con.prepareCall("{ CALL move_exercise_in_worksheet(?,?,?) }");
					cs.setInt(1, chapterId);
					cs.setInt(2, exerciseId);
					cs.setBoolean(3, up);
					return cs;
				}
		);
	}



	public int addChapter(int worksheetId, String title) {
		if (title == null || title.trim().isEmpty()) {
			throw new BadRequestException();
		}

		return jdbc.queryForObject("SELECT * FROM add_chapter_to_worksheet(?,?)",
				new Object[]{worksheetId, title}, Integer.class);
	}

	public void addExerciseToChapter(int chapterId, int exerciseId) {
		jdbc.update(
				con -> {
					CallableStatement cs = con.prepareCall("{ CALL add_exercise_to_chapter(?,?,TRUE) }");
					cs.setInt(1, chapterId);
					cs.setInt(2, exerciseId);
					return cs;
				}
		);
	}

	public void removeExerciseFromChapter(int exerciseId, int chapterId) {
		jdbc.update("DELETE FROM chapter_to_exercise WHERE chapter_id = ? AND exercise_id = ?",
				chapterId, exerciseId);
	}
}
