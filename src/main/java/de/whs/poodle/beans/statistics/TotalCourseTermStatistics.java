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
package de.whs.poodle.beans.statistics;

/*
 * Statistics for a particular CourseTerm (used for the tables in tabs
 * in courseStatistics.html / CourseStatisticsController).
 */
public class TotalCourseTermStatistics {

	private ExerciseBatchStatistics exerciseWorksheetStats;
	private ExerciseBatchStatistics selfStudyWorksheetStats;
	private ExerciseBatchStatistics total;

	private int enrolledStudentCount;

	public void setExerciseWorksheetStats(ExerciseBatchStatistics exerciseWorksheetStats) {
		this.exerciseWorksheetStats = exerciseWorksheetStats;
	}

	public void setSelfStudyWorksheetStats(ExerciseBatchStatistics selfStudyWorksheetStats) {
		this.selfStudyWorksheetStats = selfStudyWorksheetStats;
	}

	public void setTotal(ExerciseBatchStatistics total) {
		this.total = total;
	}

	public ExerciseBatchStatistics getSelfStudyWorksheetStats() {
		return selfStudyWorksheetStats;
	}

	public ExerciseBatchStatistics getExerciseWorksheetStats() {
		return exerciseWorksheetStats;
	}

	public ExerciseBatchStatistics getTotal() {
		return total;
	}

	public int getEnrolledStudentCount() {
		return enrolledStudentCount;
	}

	public void setEnrolledStudentCount(int enrolledStudentCount) {
		this.enrolledStudentCount = enrolledStudentCount;
	}
}
