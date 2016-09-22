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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import de.whs.poodle.beans.AbstractExercise.ExerciseType;
import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.CourseTagManagement;
import de.whs.poodle.beans.Tag;
import de.whs.poodle.repositories.exceptions.BadRequestException;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Repository
public class TagRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@PersistenceContext
	private EntityManager em;

	public Tag getById(int tagId) {
		return em.find(Tag.class, tagId);
	}

	/*
	 * Returns all tags belonging to any of the courses that the instructor
	 * has access to.
	 */
	@SuppressWarnings("unchecked")
	public List<Tag> getForPublicCourses(int instructorId, ExerciseType type) {
		String toTagsTable = type == ExerciseType.MC_QUESTION ? "mc_question_to_tag" : "exercise_to_tag";
		String query =
			"SELECT DISTINCT tag.*,LOWER(tag.name) name_lower FROM tag " +
			"JOIN " + toTagsTable + " totags ON tag.id = totags.tag_id " +
			"WHERE has_instructor_access_to_course(course_id,:instructorId) " +
			"ORDER BY name_lower";

		return em.createNativeQuery(query, Tag.class)
				.setParameter("instructorId", instructorId)
				.getResultList();
	}

	/*
	 * Returns all tags in the specified course. If "type" is set,
	 * only returns tags that exist on at least one exercise of the type.
	 */
	@SuppressWarnings("unchecked")
	public List<Tag> getForCourse(int courseId, ExerciseType type) {
		String query = "SELECT DISTINCT tag.*,LOWER(tag.name) name_lower FROM tag";

		if (type == ExerciseType.EXERCISE)
			query += " JOIN exercise_to_tag ON exercise_to_tag.tag_id = tag.id";
		else if (type == ExerciseType.MC_QUESTION)
			query += " JOIN mc_question_to_tag ON mc_question_to_tag.tag_id = tag.id";
		else {} // ALL

		query += " WHERE course_id = :courseId ORDER BY name_lower";

		return em.createNativeQuery(query, Tag.class)
				.setParameter("courseId", courseId)
				.getResultList();
	}

	public CourseTagManagement getCourseTagManagement(int courseId, List<Course> courses) {
		CourseTagManagement ctm = new CourseTagManagement();
		Map<Course,List<Tag>> map = new LinkedHashMap<>();

		for (Course c : courses) {
			List<Tag> tags = getForCourse(c.getId(), ExerciseType.ALL);

			if (c.getId() == courseId) {
				ctm.setCourse(c);
				ctm.setTags(tags);
			}
			else {
				map.put(c, tags);
			}
		}

		if (ctm.getCourse() == null)
			throw new NotFoundException();

		ctm.setOtherCoursesTagsMap(map);
		return ctm;
	}

	// filter duplicate tags (regarding the name, see hashCode() / equals() in Tag.java)
	public List<Tag> getDistinctTags(List<Tag> tags) {
		return tags.stream()
				.distinct()
				.collect(Collectors.toList());
	}

	public Tag createTag(Tag tag) {
		if (tag.getName().trim().isEmpty())
			throw new BadRequestException("noTagNameSpecified");

		tag.setName(tag.getName().trim());

		try {
			int id = jdbc.queryForObject(
				"INSERT INTO tag(name,course_id,instructor_only) VALUES(?,?,?) RETURNING id",
				new Object[] { tag.getName(), tag.getCourseId(), tag.getInstructorOnly() },
				Integer.class);

			tag.setId(id);
			return tag;
		} catch (DuplicateKeyException e) {
			throw new BadRequestException("tagAlreadyExists");
		}
	}

	public void deleteTag(int tagId) {
		jdbc.update("DELETE FROM tag WHERE id = ?", tagId);
	}

	public void addTagToCourse(int courseId, int tagId){
		jdbc.update("INSERT INTO tag(name,course_id,instructor_only) "
				+ "SELECT name,?,instructor_only FROM tag WHERE id = ?", courseId, tagId);
	}

	public void mergeTags(List<Integer> tagIds, int mergeTo) {
		jdbc.update(
			con -> {
				CallableStatement cs = con.prepareCall("{ CALL merge_tags(?,?) }");
				Array tagIdsArray = con.createArrayOf("int4", tagIds.toArray());
				cs.setArray(1, tagIdsArray);
				cs.setInt(2, mergeTo);
				return cs;
			}
		);
	}

	public void renameTag(int tagId, String name) {
		if (name.trim().isEmpty())
			throw new BadRequestException("noTagNameSpecified");

		name = name.trim();

		try {
			jdbc.update("UPDATE tag SET name = ? WHERE id = ?", name, tagId);
		} catch (DuplicateKeyException e) {
			throw new BadRequestException("tagAlreadyExists");
		}
	}

	public void changeInstructorOnly(int tagId, boolean instructorOnly){
		jdbc.update("UPDATE tag SET instructor_only = ? WHERE id = ?", instructorOnly, tagId);
	}

	/* Returns all tag that make sense for the exercise search for the students, i.e.
	 * all tags that are used in the specified courses or one its linked ones. */
	@SuppressWarnings("unchecked")
	public List<Tag> getForStudentInCourse(int courseId, ExerciseType type) {
		String query = "SELECT DISTINCT tag.*,LOWER(tag.name) name_lower FROM tag";

		if (type == ExerciseType.EXERCISE)
			query += " JOIN exercise_to_tag ON exercise_to_tag.tag_id = tag.id" +
					 " JOIN exercise e ON exercise_to_tag.exercise_id = e.id";
		else if (type == ExerciseType.MC_QUESTION)
			query += " JOIN mc_question_to_tag ON mc_question_to_tag.tag_id = tag.id" +
					 " JOIN mc_question e ON mc_question_to_tag.mc_question_id = e.id";
		else {
			throw new RuntimeException("type = ALL makes no sense here");
		}

		// consider this course and all linked ones
		query += " WHERE NOT instructor_only AND is_course_linked_with(:courseId,e.course_id) ORDER BY name_lower";

		return em.createNativeQuery(query, Tag.class)
				.setParameter("courseId", courseId)
				.getResultList();
	}
}
