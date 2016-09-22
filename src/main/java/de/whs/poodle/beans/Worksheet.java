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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/*
 * Base class for ExerciseWorksheet and InstructorMcWorksheet.
 */
@Entity
@Inheritance(strategy=InheritanceType.JOINED)
@Table(name="worksheet")
public abstract class Worksheet {

	public enum WorksheetType {
		EXERCISE, MC, EVALUATION;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="course_term_id")
	private CourseTerm courseTerm;

	@Column(name="unlocked")
	private boolean unlocked;

	@Column(name="unlock_at")
	private Date unlockAt;

	@Column(name="title")
	private String title;

	@Column(name="number")
	private int number; // number of the worksheet within this course term

	@Enumerated(EnumType.STRING)
	@Column(name="type", insertable=false, updatable=false)
	private WorksheetType type;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isUnlocked() {
		return unlocked;
	}

	public void setUnlocked(boolean unlocked) {
		this.unlocked = unlocked;
	}

	public Date getUnlockAt() {
		return unlockAt;
	}

	public void setUnlockAt(Date unlockAt) {
		this.unlockAt = unlockAt;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public CourseTerm getCourseTerm() {
		return courseTerm;
	}

	public void setCourseTerm(CourseTerm courseTerm) {
		this.courseTerm = courseTerm;
	}

	public WorksheetType getType() {
		return type;
	}

	public void setType(WorksheetType type) {
		this.type = type;
	}
}
