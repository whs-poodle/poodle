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

import com.fasterxml.jackson.annotation.JsonIgnore;

/*
 * A chapter in an ExerciseWorksheet.
 */
@Entity
@Table(name="chapter")
public class Chapter {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne
	@JoinColumn(name="worksheet_id")
	@JsonIgnore
	private ExerciseWorksheet worksheet;

	@Column(name="number")
	private int number;

	@Column(name="title")
	private String title;

	@OneToMany(fetch=FetchType.EAGER, mappedBy="chapterId")
	@OrderBy("number")
	private List<ExerciseInChapter> exercises;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ExerciseWorksheet getWorksheet() {
		return worksheet;
	}

	public void setWorksheet(ExerciseWorksheet worksheet) {
		this.worksheet = worksheet;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<ExerciseInChapter> getExercises() {
		return exercises;
	}

	public void setExercises(List<ExerciseInChapter> exercises) {
		this.exercises = exercises;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	@Entity
	@Table(name="chapter_to_exercise")
	public static class ExerciseInChapter {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private int id;

		@Column(name="chapter_id")
		private int chapterId;

		@Column(name="number")
		private int number; // number of the exercise within the chapter

		@ManyToOne
		@JoinColumn(name="exercise_id")
		private Exercise exercise;

		public ExerciseInChapter() {}

		public ExerciseInChapter(int number, Exercise exercise) {
			this.number = number;
			this.exercise = exercise;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getChapterId() {
			return chapterId;
		}

		public void setChapterId(int chapterId) {
			this.chapterId = chapterId;
		}

		public int getNumber() {
			return number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

		public Exercise getExercise() {
			return exercise;
		}

		public void setExercise(Exercise exercise) {
			this.exercise = exercise;
		}
	}
}
