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

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.Student;

/*
 * A McWorksheet created by a student.
 */
@Entity
@Table(name="student_mc_worksheet")
public class StudentMcWorksheet implements McWorksheet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne
	@JoinColumn(name="student_id")
	private Student student;

	@Column(name="created_at")
	private Date createdAt;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="course_term_id")
	private CourseTerm courseTerm;

	@OneToMany(mappedBy="mcWorksheetId")
	@OrderBy("number")
	private List<McQuestionOnWorksheet> questions;

	@Column(name="is_public")
	private boolean isPublic;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public int getMcWorksheetId() {
		return id;
	}

	@Override
	public List<McQuestionOnWorksheet> getQuestions() {
		return questions;
	}

	@Override
	public CourseTerm getCourseTerm() {
		return courseTerm;
	}

	public void setCourseTerm(CourseTerm courseTerm) {
		this.courseTerm = courseTerm;
	}

	public void setQuestions(List<McQuestionOnWorksheet> questions) {
		this.questions = questions;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	@Override
	public McWorksheetType getMcWorksheetType() {
		return McWorksheetType.STUDENT;
	}
}
