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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.CourseTermWorksheets;
import de.whs.poodle.beans.ExerciseWorksheet;
import de.whs.poodle.beans.Worksheet;
import de.whs.poodle.beans.Worksheet.WorksheetType;
import de.whs.poodle.beans.evaluation.EvaluationWorksheet;
import de.whs.poodle.beans.mc.InstructorMcWorksheet;
import de.whs.poodle.beans.mc.StudentMcWorksheet;
import de.whs.poodle.repositories.exceptions.BadRequestException;
import de.whs.poodle.repositories.exceptions.NotFoundException;


@Repository
public class WorksheetRepository {

	@Autowired
	private CourseTermRepository courseTermRepo;

	@Autowired
	private JdbcTemplate jdbc;

	@PersistenceContext
	private EntityManager em;

	@Transactional
	public int create(String title, int courseTermId, WorksheetType type) {
		if (title == null || title.trim().isEmpty()) {
			throw new BadRequestException();
		}

		int worksheetCount = em.createQuery(
				"SELECT COUNT(ws) FROM Worksheet ws " +
				"WHERE courseTerm.id = :courseTermId " +
				"AND CAST(type string) = :type", Long.class)
				.setParameter("courseTermId", courseTermId)
				.setParameter("type", type.name())
				.getSingleResult()
				.intValue();

		int number = worksheetCount + 1;

		int id = jdbc.queryForObject(
				"INSERT INTO worksheet(course_term_id,number,title,type) " +
				"VALUES(?,?,?,?::worksheet_type) " +
				"RETURNING id",
				new Object[]{courseTermId, number, title, type.name()},
				Integer.class);

		/* this is an instructor mc worksheet, create entries in the parent table mc_worksheet
		 * and the subtable instructor_mc_worksheet */
		if (type == WorksheetType.MC) {
			int mcWorksheetId = jdbc.queryForObject(
					"INSERT INTO mc_worksheet DEFAULT VALUES RETURNING id", Integer.class);

			jdbc.update("INSERT INTO instructor_mc_worksheet(id,mc_worksheet_id) VALUES(?,?)",
					id, mcWorksheetId);
		}
		// exercise worksheet, create entry in subtable
		else if (type == WorksheetType.EXERCISE) {
			jdbc.update("INSERT INTO exercise_worksheet(id) VALUES(?)", id);
		}

		return id;
	}

	public void changeTitle(int worksheetId, String title) {
		if (title == null || title.trim().isEmpty()) {
			throw new BadRequestException();
		}

		jdbc.update("UPDATE worksheet SET title = ? WHERE id = ?",
				title, worksheetId);
	}

	public Worksheet getById(int id) {
		Worksheet ws = em.find(Worksheet.class, id);
		if (ws == null)
			throw new NotFoundException();

		return ws;
	}

	@Transactional
	public void delete(int id) {
		Worksheet ws = em.find(Worksheet.class, id);

		jdbc.update(
				"UPDATE worksheet SET number = number - 1 " +
				"WHERE course_term_id = ? " +
				"AND type = ?::worksheet_type " +
				"AND number > ?",
				ws.getCourseTerm().getId(), ws.getType().name(), ws.getNumber());

		jdbc.update("DELETE FROM worksheet WHERE id = ?", id);
	}

	// unlock the worksheet and return whether is was actually unlocked (or already unlocked)
	public boolean unlock(int worksheetId) {
		Date date = new Date();
		return jdbc.update("UPDATE worksheet SET unlocked = TRUE, unlock_at = ? WHERE unlocked = FALSE AND id = ?", date, worksheetId) > 0;
	}

	public void setUnlockAt(int id, Date date) {
		jdbc.update("UPDATE worksheet SET unlock_at = ? WHERE id = ?", date, id);
	}

	/*
	 * returns all course terms and its worksheets for the course.
	 */
	public LinkedHashMap<CourseTerm, CourseTermWorksheets> getForCourse(int courseId) {
		LinkedHashMap<CourseTerm, CourseTermWorksheets> map = new LinkedHashMap<>();

		List<CourseTerm> courseTerms = courseTermRepo.getForCourse(courseId);

		for (CourseTerm courseTerm : courseTerms) {
			List<ExerciseWorksheet> exerciseWorksheets = em.createQuery(
					"FROM ExerciseWorksheet ws " +
					"WHERE ws.courseTerm.id = :courseTermId " +
					"ORDER BY ws.number", ExerciseWorksheet.class)
					.setParameter("courseTermId", courseTerm.getId())
					.getResultList();

			List<InstructorMcWorksheet> mcWorksheets = em.createQuery(
					"FROM InstructorMcWorksheet ws " +
					"WHERE ws.courseTerm.id = :courseTermId " +
					"ORDER BY ws.number", InstructorMcWorksheet.class)
					.setParameter("courseTermId", courseTerm.getId())
					.getResultList();

			List<StudentMcWorksheet> studentWorksheets = em.createQuery(
					"FROM StudentMcWorksheet ws " +
					"WHERE ws.courseTerm.id = :courseTermId " +
					"AND isPublic = TRUE " +
					"ORDER BY createdAt", StudentMcWorksheet.class)
					.setParameter("courseTermId", courseTerm.getId())
					.getResultList();

			EvaluationWorksheet evaluationWorksheet;

			try {
				evaluationWorksheet =
						em.createQuery("FROM EvaluationWorksheet WHERE courseTerm.id = :courseTermId", EvaluationWorksheet.class)
						.setParameter("courseTermId", courseTerm.getId())
						.getSingleResult();
			} catch(NoResultException e) {
				evaluationWorksheet = null;
			}

			CourseTermWorksheets courseTermWorksheets = new CourseTermWorksheets();
			courseTermWorksheets.setExerciseWorksheets(exerciseWorksheets);
			courseTermWorksheets.setMcWorksheets(mcWorksheets);
			courseTermWorksheets.setStudentWorksheets(studentWorksheets);
			courseTermWorksheets.setEvaluationWorksheet(evaluationWorksheet);
			map.put(courseTerm, courseTermWorksheets);
		}

		return map;
	}

	// move exercise within worksheet
	@Transactional
	public void move(int id, boolean up) {
		Worksheet ws = em.find(Worksheet.class, id);
		int number = ws.getNumber();
		int otherNumber = number + (up ? -1 : 1);

		/* Worksheet to swap with. We also check
		 * for "unlocked = FALSE" since we should not
		 * change the position of an already unlocked worksheet. */
		Worksheet otherWs;
		try {
			// the cast is necessary to compare with the psql enum
			otherWs = em.createQuery(
				"FROM Worksheet WHERE CAST(type string) = :type " +
				"AND courseTerm.id = :courseTermId " +
				"AND number = :number " +
				"AND unlocked = FALSE", Worksheet.class)
				.setParameter("type", ws.getType().name())
				.setParameter("courseTermId", ws.getCourseTerm().getId())
				.setParameter("number", otherNumber)
				.getSingleResult();
		} catch(NoResultException e) {
			// worksheet is already on top/bottom or other worksheet is already unlocked
			return;
		}

		/* Swap the numbers and unlock_at timestamps. We swap the timestamps
		 * as well because otherwise worksheet n would be unlocked after worksheet n+1 */
		em.createNativeQuery(
			"UPDATE worksheet AS ws1 " +
			"SET number = ws2.number, unlock_at = ws2.unlock_at " +
			"FROM worksheet ws2 " +
			"WHERE ws1.id IN (:id1,:id2) AND ws2.id IN (:id1,:id2) AND ws1.id != ws2.id")
			.setParameter("id1", ws.getId())
			.setParameter("id2", otherWs.getId())
			.executeUpdate();
	}

	/*
	 * Returns all coursesTerms that the student is enrolled in, together with the worksheets for each.
	 */
	public LinkedHashMap<CourseTerm, CourseTermWorksheets> getWorksheetsForStudent(int studentId) {
		LinkedHashMap<CourseTerm, CourseTermWorksheets> map = new LinkedHashMap<>();

		List<CourseTerm> courseTerms = courseTermRepo.getEnrolledForStudent(studentId);

		for (CourseTerm ct : courseTerms) {
			CourseTermWorksheets worksheets = new CourseTermWorksheets();

			List<ExerciseWorksheet> exerciseWorksheets =
					em.createQuery("FROM ExerciseWorksheet WHERE courseTerm.id = :courseTermId AND unlocked = TRUE ORDER BY number",
							ExerciseWorksheet.class)
					.setParameter("courseTermId", ct.getId())
					.getResultList();

			List<InstructorMcWorksheet> mcWorksheets =
					em.createQuery("FROM InstructorMcWorksheet WHERE courseTerm.id = :courseTermId AND unlocked = TRUE ORDER BY number",
							InstructorMcWorksheet.class)
					.setParameter("courseTermId", ct.getId())
					.getResultList();

			EvaluationWorksheet evaluationWorksheet;

			try {
				evaluationWorksheet =
						em.createQuery("FROM EvaluationWorksheet WHERE courseTerm.id = :courseTermId "
								+ "AND unlocked = TRUE AND (unlockedUntil IS NULL "
								+ "OR unlockedUntil > CURRENT_TIMESTAMP)",
								EvaluationWorksheet.class)
						.setParameter("courseTermId", ct.getId())
						.getSingleResult();
			} catch(NoResultException e) {
				evaluationWorksheet = null;
			}

			worksheets.setExerciseWorksheets(exerciseWorksheets);
			worksheets.setMcWorksheets(mcWorksheets);
			worksheets.setEvaluationWorksheet(evaluationWorksheet);

			map.put(ct, worksheets);
		}

		return map;
	}
}
