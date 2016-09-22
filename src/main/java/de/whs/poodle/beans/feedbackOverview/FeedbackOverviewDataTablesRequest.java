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

import de.whs.poodle.beans.forms.FeedbackOverviewForm.VisibleValue;

/*
 * This matches the request that DataTables sends to get the
 * rows in the Feedback Overview table. Besides the properties
 * defined by DataTables (draw, start, length) this contains
 * additional ones to return the correct rows.
 *
 * See https://www.datatables.net/manual/server-side
 * and feedbackOverview.js.
 */
public class FeedbackOverviewDataTablesRequest {

	public enum OrderDirection {
		ASC, DESC;
	}

	// defined by DataTables API
	private int draw;
	private int start;
	private int length;


	private int courseTermId;
	private VisibleValue value;

	// the exercise to sort by. If this is null, we order by student.
	private Integer orderByExerciseRootId;
	private OrderDirection orderDirection;

	public int getDraw() {
		return draw;
	}

	public void setDraw(int draw) {
		this.draw = draw;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getCourseTermId() {
		return courseTermId;
	}

	public void setCourseTermId(int courseTermId) {
		this.courseTermId = courseTermId;
	}

	public VisibleValue getValue() {
		return value;
	}

	public void setValue(VisibleValue value) {
		this.value = value;
	}

	public Integer getOrderByExerciseRootId() {
		return orderByExerciseRootId;
	}

	public void setOrderByExerciseRootId(Integer orderByExerciseRootId) {
		this.orderByExerciseRootId = orderByExerciseRootId;
	}

	public OrderDirection getOrderDirection() {
		return orderDirection;
	}

	public void setOrderDirection(OrderDirection orderDirection) {
		this.orderDirection = orderDirection;
	}

	public boolean isOrderByStudent() {
		return orderByExerciseRootId == null;
	}
}
