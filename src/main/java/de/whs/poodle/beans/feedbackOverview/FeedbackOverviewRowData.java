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
package de.whs.poodle.beans.feedbackOverview;

import java.util.Map;

import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.statistics.Statistic;

/*
 * Defines the data for one row in the Feedback Overview table.
 */
public class FeedbackOverviewRowData {

	// the student the data in this row belong to
	private Student student;

	 /* Map which maps each exercise to the statistic. If a student has not
	  * completed an exercise, there is simply no entry in the map.
	  * Note that statistics are always per "root id" since different students
	 * may have given the feedback to different revisions of one exercise. */
	private Map<Integer,Statistic> exerciseRootIdToStatisticMap;

	public FeedbackOverviewRowData(Student student, Map<Integer, Statistic> exerciseRootIdToStatisticMap) {
		this.student = student;
		this.exerciseRootIdToStatisticMap = exerciseRootIdToStatisticMap;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public Map<Integer, Statistic> getExerciseRootIdToStatisticMap() {
		return exerciseRootIdToStatisticMap;
	}

	public void setExerciseRootIdToStatisticMap(Map<Integer, Statistic> exerciseRootIdToStatisticMap) {
		this.exerciseRootIdToStatisticMap = exerciseRootIdToStatisticMap;
	}
}
