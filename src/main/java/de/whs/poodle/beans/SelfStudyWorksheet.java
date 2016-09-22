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

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

@Embeddable
public class SelfStudyWorksheet {

	/*
	 * Maximum number of exercise that can be on a worksheet.
	 */
	public static final int MAX_EXERCISES = 5;

	@OneToMany
	@JoinTable(
			name="self_study_worksheet_to_exercise",
			joinColumns={ @JoinColumn(name="student_to_course_term_id", referencedColumnName="id") },
			inverseJoinColumns={ @JoinColumn(name="exercise_id", referencedColumnName="id") }
	)
	private List<Exercise> exercises;

	public boolean isFull() {
		return exercises.size() >= MAX_EXERCISES;
	}

	public boolean isHasExercises() {
		return !exercises.isEmpty();
	}

	public int getExercisesLeft() {
		return MAX_EXERCISES - exercises.size();
	}

	public List<Exercise> getExercises() {
		return exercises;
	}

	public void setExercises(List<Exercise> exercises) {
		this.exercises = exercises;
	}
}
