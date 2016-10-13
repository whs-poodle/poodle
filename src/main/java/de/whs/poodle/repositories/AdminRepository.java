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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.Exercise;
import de.whs.poodle.beans.Instructor;

@Repository
public class AdminRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private InstructorRepository instructorRepo;

	@PersistenceContext
	private EntityManager em;

	public void changeOwner(int newOwnerId, int exerciseId) {

		int rootId = em.createQuery(
				"SELECT rootId FROM Exercise " +
				"WHERE id = :exerciseId", Integer.class)
				.setParameter("exerciseId", exerciseId)
				.getSingleResult()
				.intValue();

		jdbc.update("UPDATE exercise SET changed_by_id = ? WHERE id = ?", newOwnerId, rootId);
	}

	public List<Instructor> getInstructorsForExercise(Exercise exercise) {
		Course course = courseRepo.getById(exercise.getCourseId());

		List<Instructor> courseInstructors = new ArrayList<>();

		courseInstructors.add(course.getInstructor());

		List<Integer> ids = course.getOtherInstructorsIds();

		for (Integer instructorId : ids) {
			courseInstructors.add(instructorRepo.getById(instructorId));
		}

		courseInstructors = courseInstructors.stream()
				.filter(i -> i.getId() != exercise.getOwner().getId())
				.collect(Collectors.toList());

		return courseInstructors;
	}

	public void changeAdmins(List<Integer> instructorIds) {
		jdbc.update("UPDATE instructor SET is_admin = FALSE");

		for (Integer id : instructorIds) {
			jdbc.update("UPDATE instructor SET is_admin = TRUE WHERE id = ?", id);
		}
	}

	public void changeCourseInstructors(List<Integer> instructorIds, int courseId) {
		jdbc.update("DELETE FROM course_to_instructor WHERE course_id = ?", courseId);

		for (Integer id : instructorIds) {
			jdbc.update("INSERT INTO course_to_instructor (course_id, instructor_id) VALUES (?, ?) ON CONFLICT DO NOTHING", courseId, id);
		}
	}
}