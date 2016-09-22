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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.whs.poodle.beans.AbstractExercise.ExerciseType;
import de.whs.poodle.beans.Instructor;

@Repository
public class InstructorRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@PersistenceContext
	private EntityManager em;

	public boolean exists(String username) {
		List<Instructor> exists = em.createQuery(
				"FROM Instructor WHERE username = LOWER(:username)", Instructor.class)
				.setParameter("username", username)
				.getResultList();

		return !exists.isEmpty();
	}

	public Instructor createOrUpdate(Instructor instructor) {
		if (exists(instructor.getUsername())) {
			jdbc.update(
					"UPDATE instructor SET first_name = ?,last_name = ? " +
					"FROM poodle_user WHERE poodle_user.id = instructor.id " +
					"AND username = LOWER(?)",
					instructor.getFirstName(), instructor.getLastName(), instructor.getUsername());
			return getByUsername(instructor.getUsername());
		}
		else {
			return create(instructor);
		}
	}

	@Transactional
	public Instructor create(Instructor instructor) {
		int userId = jdbc.queryForObject(
				"INSERT INTO poodle_user(username) VALUES(LOWER(?)) " +
				"RETURNING id",
				new Object[]{instructor.getUsername()},
				Integer.class);

		if (instructor.getPasswordHash() != null)
			jdbc.update("UPDATE poodle_user SET password_hash = ? WHERE id = ?", instructor.getPasswordHash(), userId);

		int instructorId = jdbc.queryForObject(
				"INSERT INTO instructor(id,last_name,first_name,is_admin) VALUES(?,?,?,?) RETURNING id",
				new Object[]{userId, instructor.getLastName(), instructor.getFirstName(), instructor.isAdmin()},
				Integer.class);

		return getById(instructorId);
	}

	public Instructor getById(int instructorId) {
		return em.find(Instructor.class, instructorId);
	}

	public Instructor getByUsername(String username) {
		return em.createQuery("FROM Instructor WHERE username = LOWER(:username)", Instructor.class)
				.setParameter("username", username)
				.getSingleResult();
	}

	public List<Instructor> getAll() {
		return em.createQuery("FROM Instructor ORDER BY lastName", Instructor.class)
				.getResultList();
	}

	public java.util.Date updateLastLoginAndGetPrevious(int instructorId) {
		return jdbc.queryForObject(
				"UPDATE instructor SET last_login_at = NOW() " +
				"FROM instructor instructor_old " +
				"WHERE instructor.id = instructor_old.id AND instructor.id = ? " +
				"RETURNING instructor_old.last_login_at",
				new Object[]{instructorId}, java.util.Date.class);
	}

	/*
	 * Returns all instructor that created at least on exercise
	 * in any of the courses that the instructor has access to
	 * (used to display the instructor list in the search).
	 */
	public List<Instructor> getExerciseCreatorsForPublicCourses(int instructorId, ExerciseType type) {
		String table = type == ExerciseType.MC_QUESTION ? "McQuestion" : "Exercise";
		String query =
			"FROM Instructor i " +
			"WHERE EXISTS(SELECT e FROM " + table + " e WHERE e.owner.id = i.id " +
					"AND FUNCTION('has_instructor_access_to_course', e.courseId, :instructorId) = TRUE)) " +
			"ORDER BY lastName";

		return em.createQuery(query, Instructor.class)
				.setParameter("instructorId", instructorId)
				.getResultList();
	}
}
