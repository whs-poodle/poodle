/*
 * Copyright 2016 Westfälische Hochschule
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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "lecture_note")
public class LectureNote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "course_id")
	private Course course;

	@JoinColumn(name = "title")
	private String title;

	@JoinColumn(name = "groupname")
	private String groupname;

	@JoinColumn(name = "num")
	private int num;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="file_id")
	private PoodleFile poodleFile;

	public LectureNote() {}

	public LectureNote(PoodleFile poodleFile) {
		this.poodleFile = poodleFile;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Course getCourse() {
		return course;
	}

	public void setCourse(Course course) {
		this.course = course;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getGroupname() {
		return groupname;
	}

	public void setGroupname(String group) {
		this.groupname = group;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public PoodleFile getFile() {
		return poodleFile;
	}

	public void setFile(PoodleFile poodleFile) {
		this.poodleFile = poodleFile;
	}
}
