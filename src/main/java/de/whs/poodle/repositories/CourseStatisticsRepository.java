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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.statistics.CourseStatistics;
import de.whs.poodle.beans.statistics.ExerciseBatchStatistics;
import de.whs.poodle.beans.statistics.TotalCourseTermStatistics;

@Repository
public class CourseStatisticsRepository {

	@Autowired
	private CourseTermRepository courseTermRepo;

	@Autowired
	private JdbcTemplate jdbc;

	public CourseStatistics getForCourse(int courseId) {
		return jdbc.query("SELECT * FROM get_course_statistics(?)", new Object[]{courseId},

				new ResultSetExtractor<CourseStatistics>() {

					@Override
					public CourseStatistics extractData(ResultSet rs) throws SQLException {
						CourseTermStatisticsRowMapper rowMapper = new CourseTermStatisticsRowMapper();
						CourseStatistics courseStatistics = new CourseStatistics();

						while (rs.next()) {
							TotalCourseTermStatistics cts = rowMapper.mapRow(rs, 0);
							cts.setEnrolledStudentCount(rs.getInt("enrolled_students"));
							CourseTerm ct = courseTermRepo.getById(rs.getInt("course_term_id"));
							courseStatistics.addTotalCourseTermStatistics(ct, cts);
						}

						return courseStatistics;
					}

		});
	}

	public Map<LocalDate, TotalCourseTermStatistics> getDailyStatisticsForCourseTerm(int courseTermId) {
		return jdbc.query("SELECT * FROM get_daily_course_term_statistics(?)",
				new Object[]{courseTermId},

				new ResultSetExtractor<Map<LocalDate, TotalCourseTermStatistics>>() {

					@Override
					public Map<LocalDate, TotalCourseTermStatistics> extractData(ResultSet rs) throws SQLException {
						Map<LocalDate, TotalCourseTermStatistics> map = new LinkedHashMap<>();
						CourseTermStatisticsRowMapper rowMapper = new CourseTermStatisticsRowMapper();

						while (rs.next()) {
							TotalCourseTermStatistics ct = rowMapper.mapRow(rs, 0);
							LocalDate day = rs.getDate("day").toLocalDate();
							map.put(day,  ct);
						}

						return map;
					}

		});
	}

	private static class CourseTermStatisticsRowMapper implements RowMapper<TotalCourseTermStatistics> {

		@Override
		public TotalCourseTermStatistics mapRow(ResultSet rs, int rowNum) throws SQLException {
			// statistics for exercises on exercise worksheets
			ExerciseBatchStatistics exerciseWorksheetStats = new ExerciseBatchStatistics();
			exerciseWorksheetStats.setCompleted(rs.getInt("worksheet_completed"));
			exerciseWorksheetStats.setFeedback(rs.getInt("worksheet_feedbacks"));
			exerciseWorksheetStats.setStudentCount(rs.getInt("worksheet_students"));

			// statistics for exercises on self-study worksheets
			ExerciseBatchStatistics selfStudyWorksheetStats = new ExerciseBatchStatistics();
			selfStudyWorksheetStats.setCompleted(rs.getInt("self_study_completed"));
			selfStudyWorksheetStats.setFeedback(rs.getInt("self_study_feedbacks"));
			selfStudyWorksheetStats.setStudentCount(rs.getInt("self_study_students"));

			// statistic for all exercises
			ExerciseBatchStatistics total = new ExerciseBatchStatistics();
			total.setCompleted(rs.getInt("total_completed"));
			total.setFeedback(rs.getInt("total_feedbacks"));
			total.setStudentCount(rs.getInt("total_students"));

			TotalCourseTermStatistics sm = new TotalCourseTermStatistics();
			sm.setExerciseWorksheetStats(exerciseWorksheetStats);
			sm.setSelfStudyWorksheetStats(selfStudyWorksheetStats);
			sm.setTotal(total);

			return sm;
		}

	}

	public Map<LocalDate, TotalCourseTermStatistics> getStatisticsForCourse(int courseId) {
		return jdbc.query("SELECT * FROM get_course_term_statistics(?)",
				new Object[]{courseId},

				new ResultSetExtractor<Map<LocalDate, TotalCourseTermStatistics>>() {

					@Override
					public Map<LocalDate, TotalCourseTermStatistics> extractData(ResultSet rs) throws SQLException {
						Map<LocalDate, TotalCourseTermStatistics> map = new LinkedHashMap<>();
						CourseTermStatisticsRowMapper rowMapper = new CourseTermStatisticsRowMapper();

						while (rs.next()) {
							TotalCourseTermStatistics ct = rowMapper.mapRow(rs, 0);
							LocalDate day = rs.getDate("day").toLocalDate();
							map.put(day,  ct);
						}

						return map;
					}

		});
	}

	public void createTotalCourseStatistic(CourseStatistics courseStatistics) {
		CourseTerm courseTerm = new CourseTerm();
		courseTerm.setId(0);

		TotalCourseTermStatistics newStats = new TotalCourseTermStatistics();
		newStats.setExerciseWorksheetStats(new ExerciseBatchStatistics());
		newStats.setSelfStudyWorksheetStats(new ExerciseBatchStatistics());
		newStats.setTotal(new ExerciseBatchStatistics());

		for (Map.Entry<CourseTerm, TotalCourseTermStatistics> entry : courseStatistics.getCourseTermMap().entrySet()) {
			TotalCourseTermStatistics value = entry.getValue();

			// statistics for exercises on exercise worksheets
			ExerciseBatchStatistics oldExerciseWorksheetStats = value.getExerciseWorksheetStats();
			ExerciseBatchStatistics exerciseWorksheetStats = newStats.getExerciseWorksheetStats();
			exerciseWorksheetStats.setCompleted(exerciseWorksheetStats.getCompleted() + oldExerciseWorksheetStats.getCompleted());
			exerciseWorksheetStats.setFeedback(exerciseWorksheetStats.getFeedback() + oldExerciseWorksheetStats.getFeedback());
			exerciseWorksheetStats.setStudentCount(exerciseWorksheetStats.getStudentCount() + oldExerciseWorksheetStats.getStudentCount());

			// statistics for exercises on self-study worksheets
			ExerciseBatchStatistics oldSelfStudyWorksheetStats = value.getSelfStudyWorksheetStats();
			ExerciseBatchStatistics selfStudyWorksheetStats = newStats.getSelfStudyWorksheetStats();
			selfStudyWorksheetStats.setCompleted(selfStudyWorksheetStats.getCompleted() + oldSelfStudyWorksheetStats.getCompleted());
			selfStudyWorksheetStats.setFeedback(selfStudyWorksheetStats.getFeedback() + oldSelfStudyWorksheetStats.getFeedback());
			selfStudyWorksheetStats.setStudentCount(selfStudyWorksheetStats.getStudentCount() + oldSelfStudyWorksheetStats.getStudentCount());

			// statistic for all exercises
			ExerciseBatchStatistics oldTotal = value.getTotal();
			ExerciseBatchStatistics total = newStats.getTotal();
			total.setCompleted(total.getCompleted() + oldTotal.getCompleted());
			total.setFeedback(total.getFeedback() + oldTotal.getFeedback());
			total.setStudentCount(total.getStudentCount() + oldTotal.getStudentCount());

			newStats.setExerciseWorksheetStats(exerciseWorksheetStats);
			newStats.setSelfStudyWorksheetStats(selfStudyWorksheetStats);
			newStats.setTotal(total);
		}
		
		courseStatistics.addTotalCourseTermStatistics(courseTerm, newStats);
	}
}
