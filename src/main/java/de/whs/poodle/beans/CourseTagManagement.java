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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/*
 * Wrapper object used in TagsController
 * to display the course, its tags and the tags
 * of the other courses.
 */
public class CourseTagManagement implements Serializable {

	private static final long serialVersionUID = 1L;

	private Course course;
	private List<Tag> tags;

	private Map<Course,List<Tag>> otherCoursesTagsMap;

	public Course getCourse() {
		return course;
	}

	public void setCourse(Course course) {
		this.course = course;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public Map<Course, List<Tag>> getOtherCoursesTagsMap() {
		return otherCoursesTagsMap;
	}

	public void setOtherCoursesTagsMap(Map<Course, List<Tag>> otherCoursesTagsMap) {
		this.otherCoursesTagsMap = otherCoursesTagsMap;
	}

	public boolean hasTag(String name) {
		return tags.stream()
				.anyMatch(t -> t.getName().equals(name));
	}

	public boolean isHasOtherCourses() {
		return !otherCoursesTagsMap.isEmpty();
	}
}
