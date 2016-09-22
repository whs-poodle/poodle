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
package de.whs.poodle.beans.statistics;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.whs.poodle.beans.CompletionStatus;
import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.Exercise;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.Student;

/*
 * A Statistic given by a student for a particular exercise.
 */
@Entity
@Table(name="v_statistic")
public class Statistic {

	// must be in sync with java enum
	public enum StatisticSource {
		EXERCISE_WORKSHEET, SELF_STUDY;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(name="completion_status")
	@Enumerated(EnumType.STRING)
	private CompletionStatus status;

	@Column(name="difficulty")
	private Integer difficulty;

	@Column(name="fun")
	private Integer fun;

	@Column(name="time")
	private Integer time;

	@Column(name="text")
	private String text;

	@Column(name="completed_at")
	private Date completedAt;

	@Column(name="source")
	@Enumerated(EnumType.STRING)
	private StatisticSource source;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="exercise_id")
	@JsonIgnore
	private Exercise exercise;

	@Column(name="exercise_root_id", insertable=false)
	private int exerciseRootId;

	@ManyToOne
	@JoinColumn(name="student_id")
	private Student student;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="course_term_id")
	@JsonIgnore
	private CourseTerm courseTerm;

	@Column(name="empty", insertable=false)
	private boolean empty;

	@OneToOne
	@JoinColumn(name="id")
	private InstructorComment comment;

	@Column(name="ignore")
	private boolean ignore;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public CompletionStatus getStatus() {
		return status;
	}

	public void setStatus(CompletionStatus status) {
		this.status = status;
	}

	public Integer getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(Integer difficulty) {
		this.difficulty = difficulty;
	}

	public Integer getFun() {
		return fun;
	}

	public void setFun(Integer fun) {
		this.fun = fun;
	}

	public Integer getTime() {
		return time;
	}

	public void setTime(Integer time) {
		this.time = time;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Date getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Date completedAt) {
		this.completedAt = completedAt;
	}

	public StatisticSource getSource() {
		return source;
	}

	public void setSource(StatisticSource source) {
		this.source = source;
	}

	public Exercise getExercise() {
		return exercise;
	}

	public void setExercise(Exercise exercise) {
		this.exercise = exercise;
	}

	public int getExerciseRootId() {
		return exerciseRootId;
	}

	public void setExerciseRootId(int exerciseRootId) {
		this.exerciseRootId = exerciseRootId;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public CourseTerm getCourseTerm() {
		return courseTerm;
	}

	public void setCourseTerm(CourseTerm courseTerm) {
		this.courseTerm = courseTerm;
	}

	public InstructorComment getComment() {
		return comment;
	}

	public void setComment(InstructorComment comment) {
		this.comment = comment;
	}

	public boolean isIgnore() {
		return ignore;
	}

	public void setIgnore(boolean ignore) {
		this.ignore = ignore;
	}

	public boolean isEmpty() {
		return empty;
	}

	public void setEmpty(boolean empty) {
		this.empty = empty;
	}

	public boolean isCommented() {
		return comment != null && comment.getText() != null;
	}

	@Entity
	@Table(name="statistic_instructor_comment")
	public static class InstructorComment {

		@Id
		@Column(name="statistic_id")
		private int statisticId;

		/* Don't lazy fetch this since Jackson can't serialize it otherwise
		 * and we need this property in almost all cases anyway. */
		@ManyToOne(fetch=FetchType.EAGER)
		@JoinColumn(name = "instructor_id")
		@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
		private Instructor instructor;

		@Column(name="text")
		private String text;

		@Column(name="seen")
		private boolean seen;

		public int getStatisticId() {
			return statisticId;
		}

		public void setStatisticId(int statisticId) {
			this.statisticId = statisticId;
		}

		public Instructor getInstructor() {
			return instructor;
		}

		public void setInstructor(Instructor instructor) {
			this.instructor = instructor;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public boolean isSeen() {
			return seen;
		}

		public void setSeen(boolean seen) {
			this.seen = seen;
		}
	}
}
