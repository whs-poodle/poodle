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

public class McQuestionSearchCriteria {

	public enum Order {
		TEXT("text"),
		DATE("created_at");

		// string used in the query (ORDER BY ..)
		private String dbString;

		private Order(String dbString) {
			this.dbString = dbString;
		}

		public String getDbString() {
			return this.dbString;
		}
	}

	private int tags[];
	private int courses[];
	private int instructors[];

	private Order order;
	private boolean orderAscending;

	private String text;

	private Integer instructorId;

	private boolean tagsAnd; // AND or OR search

	/* if the search is in "add to worksheet" mode, this
	 * id is used to filter out questions that already exist
	 * on the worksheet. */
	private Integer filterInstructorMcWorksheetId;

	public McQuestionSearchCriteria() {
		this.tags = new int[0];
		this.courses = new int[0];
		this.instructors = new int[0];
		this.order = Order.TEXT;
		this.orderAscending = true;
		this.text = "";
		this.tagsAnd = true;
	}

	public boolean isTagsAnd() {
		return tagsAnd;
	}

	public void setTagsAnd(boolean tagsAnd) {
		this.tagsAnd = tagsAnd;
	}

	public Integer getInstructorId() {
		return instructorId;
	}

	public void setInstructorId(Integer instructorId) {
		this.instructorId = instructorId;
	}

	public int[] getTags() {
		return tags;
	}

	public void setTags(int[] tags) {
		this.tags = tags;
	}

	public int[] getCourses() {
		return courses;
	}

	public void setCourses(int[] courses) {
		this.courses = courses;
	}

	public int[] getInstructors() {
		return instructors;
	}

	public void setInstructors(int[] instructors) {
		this.instructors = instructors;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public boolean isOrderAscending() {
		return orderAscending;
	}

	public void setOrderAscending(boolean orderAscending) {
		this.orderAscending = orderAscending;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Integer getFilterInstructorMcWorksheetId() {
		return filterInstructorMcWorksheetId;
	}

	public void setFilterInstructorMcWorksheetId(Integer filterInstructorMcWorksheetId) {
		this.filterInstructorMcWorksheetId = filterInstructorMcWorksheetId;
	}

	public boolean isEmpty() {
		return tags.length == 0 && courses.length == 0 &&
				instructors.length == 0 && text.isEmpty();

	}
}
