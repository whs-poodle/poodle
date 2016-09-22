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

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import de.whs.poodle.beans.StudentToCourseTerm;

@Repository
public class StudentToCourseTermRepository {

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;

	public void addExercise(int studentId, int courseTermId, int exerciseId) {
		jdbc.update(
				"INSERT INTO self_study_worksheet_to_exercise(student_to_course_term_id,exercise_id) " +
				"SELECT id,? FROM student_to_course_term WHERE student_id = ? AND course_term_id = ?",
				exerciseId, studentId, courseTermId);
	}

	public StudentToCourseTerm get(int studentId, int courseTermId) {
		try {
			return em.createQuery(
				"FROM StudentToCourseTerm WHERE student.id = :studentId AND courseTerm.id = :courseTermId",
				StudentToCourseTerm.class)
				.setParameter("studentId", studentId)
				.setParameter("courseTermId", courseTermId)
				.getSingleResult();
		} catch(NoResultException e) {
			return null;
		}
	}

	public void removeExerciseFromWorksheet(int studentId, int courseTermId, int exerciseId) {
		jdbc.update(
				"DELETE FROM self_study_worksheet_to_exercise swe " +
				"USING student_to_course_term sct " +
				"WHERE swe.student_to_course_term_id = sct.id " +
				"AND student_id = ? AND course_term_id = ? AND exercise_id = ?",
				studentId, courseTermId, exerciseId);
	}
}
