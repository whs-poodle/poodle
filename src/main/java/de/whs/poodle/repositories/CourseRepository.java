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

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import de.whs.poodle.beans.Course;
import de.whs.poodle.repositories.exceptions.BadRequestException;

@Repository
public class CourseRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@PersistenceContext
	private EntityManager em;

	@SuppressWarnings("unchecked")
	public List<Course> getAllForInstructor(int instructorId) {
		// native query because ORDER BY (instructor_id	 = :instructorId) is not possible with JPQL
		return em.createNativeQuery(
				"SELECT * FROM course " +
				"WHERE has_instructor_access_to_course(course.id, :instructorId) " +
				"ORDER BY (instructor_id = :instructorId) DESC, name", Course.class)
				.setParameter("instructorId", instructorId)
				.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<Course> getAll() {
		return em.createNativeQuery("SELECT * FROM course ORDER BY name", Course.class).getResultList();
	}

	public Course getById(int id) {
		return em.find(Course.class, id);
	}

	public void edit(Course course) {
		try {
			jdbc.update(
				con -> {
					CallableStatement cs = con.prepareCall("{ CALL update_course(?,?,?,?,?) }");
					cs.setInt(1, course.getId());
					cs.setString(2, course.getName());
					if (course.getPassword().trim().isEmpty())
						cs.setNull(3, Types.VARCHAR);
					else
						cs.setString(3, course.getPassword());

					Array otherInstructors = con.createArrayOf("int4", course.getOtherInstructorsIds().toArray());
					cs.setArray(4, otherInstructors);
					Array linkedCourses = con.createArrayOf("int4", course.getLinkedCoursesIds().toArray());
					cs.setArray(5, linkedCourses);
					return cs;
				});
		} catch(DuplicateKeyException e) {
			throw new BadRequestException();
		}
	}


	public void create(Course course, String firstTermName) {
		try {
			int id = jdbc.query(
				con -> {
					// the function creates the course and the first term (firstTermId)
					PreparedStatement ps = con.prepareStatement("SELECT * FROM create_course(?,?,?,?,?,?)");
					ps.setInt(1, course.getInstructor().getId());
					ps.setString(2, course.getName());
					if (course.getPassword().trim().isEmpty())
						ps.setNull(3, Types.VARCHAR);
					else
						ps.setString(3, course.getPassword());

					Array otherInstructors = con.createArrayOf("int4", course.getOtherInstructorsIds().toArray());
					ps.setArray(4, otherInstructors);
					Array linkedCourses = con.createArrayOf("int4", course.getLinkedCoursesIds().toArray());
					ps.setArray(5, linkedCourses);
					ps.setString(6, firstTermName);
					return ps;
				},
				new ResultSetExtractor<Integer>() {

					@Override
					public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
						rs.next();
						return rs.getInt(1);
					}
				}
			);

			course.setId(id);
		} catch(DuplicateKeyException e) {
			throw new BadRequestException();
		}
	}

	public void delete(int courseId) {
		jdbc.update("DELETE FROM course WHERE id = ?", courseId);
	}

	@SuppressWarnings("unchecked")
	public List<Course> getLinkedCurses(int courseId) {
		return em.createNativeQuery(
				"SELECT course.* FROM course join course_to_linked_course lc"
			  + " ON course.id = lc.linked_course_id WHERE lc.course_id = :course_id",
				Course.class)
				.setParameter("course_id", courseId)
				.getResultList();
	}
}
