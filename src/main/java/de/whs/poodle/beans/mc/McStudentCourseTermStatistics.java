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
package de.whs.poodle.beans.mc;

/*
 * Contains information on how many questions the student has already
 * answered in a particular course term. This data is displayed
 * in multipleChoice.html / MultipleChoiceController.
 */
public class McStudentCourseTermStatistics {

	private int answeredCount;
	private int totalCount;

	public int getAnsweredCount() {
		return answeredCount;
	}

	public void setAnsweredCount(int answeredCount) {
		this.answeredCount = answeredCount;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public boolean isAllAnswered() {
		return totalCount == answeredCount;
	}
}
