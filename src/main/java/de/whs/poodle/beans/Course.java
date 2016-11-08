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
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.validator.constraints.NotBlank;

@Entity
@Table(name = "course")
public class Course implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "instructor_id")
	private Instructor instructor;

	@Column(name="name")
	@NotBlank(message="{noNameSpecified}")
	private String name;

	@Column(name="visible")
	private boolean visible;

	@Column(name="password")
	private String password;

	@ElementCollection
	@CollectionTable(
			name ="course_to_instructor",
			joinColumns=@JoinColumn(name="course_id")
		)
	@Column(name="instructor_id")
	private List<Integer> otherInstructorsIds;

	@ElementCollection
	@CollectionTable(
			name ="course_to_linked_course",
			joinColumns=@JoinColumn(name="course_id")
		)
	@Column(name="linked_course_id")
	private List<Integer> linkedCoursesIds;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Instructor getInstructor() {
		return instructor;
	}

	public void setInstructor(Instructor instructor) {
		this.instructor = instructor;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean getVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isHasPassword() {
		return password != null && !password.isEmpty();
	}

	public List<Integer> getOtherInstructorsIds() {
		if (otherInstructorsIds == null)
			otherInstructorsIds = new ArrayList<>();

		return otherInstructorsIds;
	}

	public void setOtherInstructorsIds(List<Integer> otherInstructorsIds) {
		this.otherInstructorsIds = otherInstructorsIds;
	}

	public List<Integer> getLinkedCoursesIds() {
		if (linkedCoursesIds == null)
			linkedCoursesIds = new ArrayList<>();

		return linkedCoursesIds;
	}

	public void setLinkedCoursesIds(List<Integer> linkedCoursesIds) {
		this.linkedCoursesIds = linkedCoursesIds;
	}

	@Override
	public String toString() {
		return getNameWithInstructor();
	}

	public String getNameWithInstructor() {
		return this.name + " (" + instructor.getLastName() + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result
				+ ((instructor == null) ? 0 : instructor.hashCode());
		result = prime
				* result
				+ ((linkedCoursesIds == null) ? 0 : linkedCoursesIds.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime
				* result
				+ ((otherInstructorsIds == null) ? 0 : otherInstructorsIds
						.hashCode());
		result = prime * result
				+ ((password == null) ? 0 : password.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Course other = (Course) obj;
		if (id != other.id)
			return false;
		if (instructor == null) {
			if (other.instructor != null)
				return false;
		} else if (!instructor.equals(other.instructor))
			return false;
		if (linkedCoursesIds == null) {
			if (other.linkedCoursesIds != null)
				return false;
		} else if (!linkedCoursesIds.equals(other.linkedCoursesIds))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (otherInstructorsIds == null) {
			if (other.otherInstructorsIds != null)
				return false;
		} else if (!otherInstructorsIds.equals(other.otherInstructorsIds))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		return true;
	}
}
