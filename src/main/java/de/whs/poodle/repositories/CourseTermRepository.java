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

import de.whs.poodle.beans.CourseTerm;

@Repository
public class CourseTermRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@PersistenceContext
	private EntityManager em;

	public CourseTerm getById(int id) {
		return em.find(CourseTerm.class, id);
	}

	public List<CourseTerm> getForCourse(int courseId) {
		return em.createQuery("FROM CourseTerm WHERE course.id = :courseId ORDER BY id DESC", CourseTerm.class)
				.setParameter("courseId", courseId)
				.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<CourseTerm> getEnrolledForStudent(int studentId) {
		return em.createNativeQuery(
			"SELECT sm.* FROM student_to_course_term shs " +
			"JOIN v_course_term sm ON shs.course_term_id = sm.id " +
			"WHERE shs.student_id = :studentId " +
			"ORDER BY sm.id DESC", CourseTerm.class)
			.setParameter("studentId", studentId)
			.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<CourseTerm> getNotEnrolledForStudent(int studentId) {
		return em.createNativeQuery(
			"SELECT sm.* FROM v_course_term sm " +
			"LEFT JOIN student_to_course_term shs ON shs.course_term_id = sm.id AND shs.student_id = :studentId " +
			"WHERE shs.course_term_id IS NULL AND sm.is_latest " +
			"ORDER BY sm.id DESC", CourseTerm.class)
			.setParameter("studentId", studentId)
			.getResultList();
	}

	public void enrollStudent(int studentId, int courseTermId) {
		jdbc.update(
			"INSERT INTO student_to_course_term(student_id,course_term_id) VALUES(?,?)",
			studentId, courseTermId);
	}

	public void unenrollStudent(int studentId, int courseTermid) {
		jdbc.update(
			"DELETE FROM student_to_course_term WHERE student_id = ? AND course_term_id = ?",
			studentId, courseTermid);
	}

	public List<String> getEmailMessageRecipients(int courseTermId) {
		return jdbc.queryForList(
				"SELECT username FROM v_student " +
				"JOIN student_to_course_term shs ON shs.student_id = v_student.id " +
				"WHERE shs.course_term_id = ? AND v_student.cfg_email_messages",
				new Object[]{courseTermId}, String.class);
	}

	public List<String> getWorksheetUnlockedEmailRecipients(int courseTermId) {
		return jdbc.queryForList(
				"SELECT username FROM v_student " +
				"JOIN student_to_course_term shs ON shs.student_id = v_student.id " +
				"WHERE shs.course_term_id = ? AND v_student.cfg_email_worksheet_unlocked",
				new Object[]{courseTermId}, String.class);
	}
}
