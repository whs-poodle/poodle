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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.StudentConfig;

@Repository
public class StudentRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManager em;

	public boolean studentExists(String username) {
		return getByUsername(username) != null;
	}

	@Transactional
	public Student createIfNotExistsAndGet(String username) {
		if (!studentExists(username)) {
			int userId = jdbc.queryForObject(
					"INSERT INTO poodle_user(username) VALUES(?) RETURNING id",
					new Object[]{username}, Integer.class);

			jdbc.update("INSERT INTO student(id) VALUES(?)", userId);
		}

		return getByUsername(username);
	}

	@Transactional
	public Student create(Student student) {
		int userId = jdbc.queryForObject(
				"INSERT INTO poodle_user(username,password_hash) VALUES(?,?) RETURNING id",
				new Object[]{student.getUsername(), student.getPasswordHash()},
				Integer.class);

		jdbc.update("INSERT INTO student(id) VALUES(?)", userId);

		student.setId(userId);
		return student;
	}

	public Student getByUsername(String username) {
		try {
			return em.createQuery("FROM Student WHERE username = LOWER(:username)", Student.class)
				.setParameter("username", username)
				.getSingleResult();
		} catch(NoResultException e) {
			return null;
		}
	}

	public Student getById(int id) {
		return em.find(Student.class, id);
	}

	public Student createFakeStudent(int instructorId) {
		int studentId = (Integer)em.createNativeQuery("SELECT * FROM create_fake_student(:instructorId)")
				.setParameter("instructorId", instructorId)
				.getSingleResult();

		return getById(studentId);
	}

	public boolean dropFakeStudent(int instructorId) {
		int n = jdbc.update(
				"DELETE FROM poodle_user USING student " +
				"WHERE student.id = poodle_user.id AND student.fake_for_instructor_id = ?", instructorId);
		return n > 0;
	}

	public void updateConfigForStudent(int id, StudentConfig cfg) {
		jdbc.update(
				"UPDATE student SET cfg_email_worksheet_unlocked = ?, cfg_email_messages = ? WHERE id = ?",
				cfg.isEmailWorksheetUnlocked(), cfg.isEmailMessages(), id);
	}
}
