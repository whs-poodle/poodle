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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import de.whs.poodle.beans.Chapter;
import de.whs.poodle.beans.Chapter.ExerciseInChapter;
import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.ExerciseWorksheet;
import de.whs.poodle.beans.forms.FeedbackSearchCriteria;
import de.whs.poodle.beans.statistics.Statistic;
import de.whs.poodle.beans.statistics.StatisticList;
import de.whs.poodle.beans.statistics.WorksheetStatistics;

@Repository
public class StatisticsRepository {

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;

	public StatisticList getForExerciseRoot(int exerciseRootId) {
		List<Statistic> list = em.createQuery(
				"FROM Statistic s WHERE s.exerciseRootId = :rootId ORDER BY s.completedAt DESC", Statistic.class)
				.setParameter("rootId", exerciseRootId)
				.getResultList();

		return new StatisticList(list);
	}

	public WorksheetStatistics getWorksheetStatistics(ExerciseWorksheet worksheet) {
		WorksheetStatistics ws = new WorksheetStatistics();

		for (Chapter c : worksheet.getChapters()) {
			for (ExerciseInChapter ec : c.getExercises()) {
				int exerciseRootId = ec.getExercise().getRootId();
				StatisticList exerciseStatisticList = getForExerciseOnWorksheet(exerciseRootId, worksheet.getCourseTerm().getId());
				ws.addExerciseStatistic(exerciseRootId, exerciseStatisticList);
			}
		}

		int enrolledCount = jdbc.queryForObject(
				"SELECT COUNT(1) FROM student_to_course_term WHERE course_term_id = ?",
				new Object[]{worksheet.getCourseTerm().getId()},
				Integer.class);

		ws.setEnrolledCount(enrolledCount);

		return ws;
	}

	public void setInstructorCommentSeen(int statisticId) {
		jdbc.update("UPDATE statistic_instructor_comment SET seen = TRUE WHERE statistic_id = ?", statisticId);
	}

	public List<Statistic> getStatistics(FeedbackSearchCriteria s, int instructorId, int limit) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Statistic> cq = cb.createQuery(Statistic.class);
		Root<Statistic> statistic = cq.from(Statistic.class);

		/* fetch the exercises immediately so they don't have to
		 * be lazy-fetched one by one while rendering the table rows. */
		statistic.fetch("exercise");

		// join we need to match the course
		Join<Statistic,Course> course = statistic.join("courseTerm").join("course");

		// create empty "and" predicate
		Predicate where = cb.conjunction();

		// only non-empty
		Predicate notEmpty = cb.isFalse(statistic.get("empty"));
		where = cb.and(where, notEmpty);

		// only statistics for courses that the instructor has access to
		Predicate hasInstructorAccessToCourse =
				cb.isTrue(
					cb.function("has_instructor_access_to_course",
							Boolean.class, course.get("id"), cb.literal(instructorId)
					)
				);

		where = cb.and(where, hasInstructorAccessToCourse);

		// filter by courseId
		if (s.getCourseId() != 0) {
			Predicate courseMatches = cb.equal(course.get("id"), s.getCourseId());
			where = cb.and(where, courseMatches);
		}

		// filter by student
		if (s.getStudent() != null) {
			Predicate studentMatches = cb.equal(statistic.get("student").get("id"), s.getStudent().getId());
			where = cb.and(where, studentMatches);
		}

		cq.where(where);

		// order by date
		cq.orderBy(cb.desc(statistic.get("completedAt")));

		return em.createQuery(cq)
				.setMaxResults(limit)
				.getResultList();
	}

	public Map<Integer,Statistic> getExerciseToStatisticMapForWorksheet(int studentId, int worksheetId) {
		@SuppressWarnings("unchecked")
		List<Statistic> statistics =
				em.createNativeQuery(
				"SELECT s.* FROM worksheet ws " +
				"JOIN chapter c ON c.worksheet_id = ws.id " +
				"JOIN chapter_to_exercise ce ON ce.chapter_id = c.id " +
				"JOIN exercise e ON ce.exercise_id = e.id " +
				"JOIN v_statistic s ON s.exercise_root_id = e.root_id " +
				"WHERE ws.id = :worksheetId AND s.student_id = :studentId",
				Statistic.class)
				.setParameter("worksheetId", worksheetId)
				.setParameter("studentId", studentId)
				.getResultList();

		return statistics.stream().collect(
				Collectors.toMap(Statistic::getExerciseRootId, Function.identity())
		);
	}

	public Map<Integer,Statistic> getExerciseToStatisticMapForSelfStudy(int studentId, int courseTermId) {
		@SuppressWarnings("unchecked")
		List<Statistic> statistics =
				em.createNativeQuery(
				"SELECT s.* FROM self_study_worksheet_to_exercise wse " +
				"JOIN student_to_course_term sct ON sct.id = wse.student_to_course_term_id " +
				"JOIN exercise e ON wse.exercise_id = e.id " +
				"JOIN v_statistic s ON s.exercise_root_id = e.root_id AND sct.student_id = s.student_id " +
				"WHERE s.student_id = :studentId AND sct.course_term_id = :courseTermId",
				Statistic.class)
				.setParameter("courseTermId", courseTermId)
				.setParameter("studentId", studentId)
				.getResultList();

		return statistics.stream().collect(
				Collectors.toMap(Statistic::getExerciseRootId, Function.identity())
		);
	}

	public List<Statistic> getForWorksheetsInCourseTerm(int courseTermId) {
		return em.createQuery(
				"FROM Statistic s WHERE s.courseTerm.id = :courseTermId " +
				"AND s.completedAt IS NOT NULL AND CAST(source string) = 'EXERCISE_WORKSHEET' AND s.empty = FALSE", Statistic.class)
				.setParameter("courseTermId", courseTermId)
				.getResultList();
	}

	public boolean commentOnFeedback(int statisticId, int instructorId, String comment) {
		try {
			jdbc.update(
				"INSERT INTO statistic_instructor_comment(statistic_id,text,instructor_id) VALUES(?,?,?)",
				statisticId, comment, instructorId);
			return true;
		} catch(DuplicateKeyException e) {
			// comment already existed
			return false;
		}
	}

	public Statistic getById(int id) {
		return em.find(Statistic.class, id);
	}

	public Statistic getForExerciseAndStudent(int exerciseRootId, int studentId) {
		try {
			return em.createQuery("FROM Statistic WHERE student.id = :studentId AND exercise.rootId = :rootId", Statistic.class)
					.setParameter("studentId", studentId)
					.setParameter("rootId", exerciseRootId)
					.getSingleResult();
		} catch(NoResultException e) { // student has not completed this exercise yet
			return null;
		}
	}

	public List<Statistic> getStatisticsWithNewCommentsForStudent(int studentId) {
		return em.createQuery(
				"FROM Statistic WHERE student.id = :studentId " +
				"AND comment.text IS NOT NULL AND comment.seen = FALSE",
				Statistic.class)
				.setParameter("studentId", studentId)
				.getResultList();
	}

	public void setIgnoreStatistic(int statisticId, boolean ignore) {
		jdbc.update("UPDATE statistic SET ignore = ? WHERE id = ?", ignore, statisticId);
	}

	public StatisticList getForExerciseOnWorksheet(int exerciseRootId, int courseTermId) {
		List<Statistic> list =
				em.createQuery(
					"FROM Statistic WHERE CAST(source string) = 'EXERCISE_WORKSHEET' " +
					"AND exerciseRootId = :exerciseRootId " +
					"AND courseTerm.id = :courseTermId", Statistic.class)
					.setParameter("exerciseRootId", exerciseRootId)
					.setParameter("courseTermId", courseTermId)
					.getResultList();

		return new StatisticList(list);
	}
}
