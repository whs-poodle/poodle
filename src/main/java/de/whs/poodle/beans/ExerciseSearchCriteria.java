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
package de.whs.poodle.beans;


/*
 * Form for the exercise search.
 */
public class ExerciseSearchCriteria {

	// Defines how the difficulty is searched for
	public enum DifficultyMode {
		NONE, MIN, MAX
	}

	public enum Order {
		TITLE("title"),
		TEXT("text"),
		DATE("created_at"),
		RANDOM("RANDOM()"),
		FEEDBACK_COUNT("feedback_count"),
		COMPLETED_COUNT("completed_count"),
		AVG_TIME("avg_time"),
		AVG_DIFFICULTY("avg_difficulty"),
		AVG_FUN("avg_fun");

		// column added to query string (ORDER BY..)
		private String dbString;

		private Order(String dbString) {
			this.dbString = dbString;
		}

		public String getDbString() {
			return this.dbString;
		}
	}

	private int[] courses = new int[0];
	private int[] tags = new int[0];
	private int[] instructors = new int[0];

	private int difficulty;
	private DifficultyMode difficultyMode = DifficultyMode.NONE;

	private String text = "";

	private boolean searchTitle = true;
	private boolean searchText = true;

	/*
	 *	This is a courseTerm ID that is set if the instructor
	 *	add exercises to an exercise worksheet. It is used
	 *	to filter out all exercises that already exist on
	 *	one of the worksheets in the course term.
	 */
	private Integer worksheetFilter;

	private StudentFilter studentFilter = new StudentFilter();

	private Order order = null;
	private boolean orderAscending = true;

	private boolean withFeedback = false;

	private Integer instructorId;

	private boolean tagsAnd = true;

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

	public int[] getCourses() {
		return courses;
	}

	public void setCourses(int[] courses) {
		this.courses = courses;
	}

	public int[] getTags() {
		return tags;
	}

	public void setTags(int[] tags) {
		this.tags = tags;
	}

	public int[] getInstructors() {
		return instructors;
	}

	public void setInstructors(int[] instructors) {
		this.instructors = instructors;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}

	public DifficultyMode getDifficultyMode() {
		return difficultyMode;
	}

	public void setDifficultyMode(DifficultyMode difficultyMode) {
		this.difficultyMode = difficultyMode;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isSearchTitle() {
		return searchTitle;
	}

	public void setSearchTitle(boolean searchTitle) {
		this.searchTitle = searchTitle;
	}

	public boolean isSearchText() {
		return searchText;
	}

	public void setSearchText(boolean searchText) {
		this.searchText = searchText;
	}

	public Integer getWorksheetFilter() {
		return worksheetFilter;
	}

	public void setWorksheetFilter(Integer worksheetFilter) {
		this.worksheetFilter = worksheetFilter;
	}

	public StudentFilter getStudentFilter() {
		return studentFilter;
	}

	public void setStudentFilter(StudentFilter studentFilter) {
		this.studentFilter = studentFilter;
	}

	public Order getOrder() {
		if (order != null) {
			return order;
		}
		else {
			// default value for student / instructor
			return isStudent() ? Order.RANDOM : Order.TITLE;
		}
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public boolean isEmpty() {
		return courses.length == 0 && tags.length == 0 &&
			   instructors.length == 0 && !withFeedback &&
			   (text.isEmpty() || (!searchTitle && !searchText)) &&
			   (difficultyMode == DifficultyMode.NONE || difficulty == 0);
	}

	public boolean isStudent() {
		return this.studentFilter.getId() != 0;
	}

	public boolean isOrderAscending() {
		return orderAscending;
	}

	public void setOrderAscending(boolean orderAscending) {
		this.orderAscending = orderAscending;
	}

	public boolean isWithFeedback() {
		return withFeedback;
	}

	public void setWithFeedback(boolean withFeedback) {
		this.withFeedback = withFeedback;
	}


	public static class StudentFilter {

		/*
		 * Similiar to worksheetFilter, this makes sure
		 * that exercises that already exist on the students'
		 * self study worksheet, are filtered from the results
		 * (we need both IDs to identify the worksheet).
		 */
		private int id;
		private int courseTermId;

		// filter already completed exercises
		private boolean hideIfAlreadyCompleted = true;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getCourseTermId() {
			return courseTermId;
		}

		public void setCourseTermId(int courseTermId) {
			this.courseTermId = courseTermId;
		}

		public boolean isHideIfAlreadyCompleted() {
			return hideIfAlreadyCompleted;
		}

		public void setHideIfAlreadyCompleted(boolean hideIfAlreadyCompleted) {
			this.hideIfAlreadyCompleted = hideIfAlreadyCompleted;
		}
	}
}
