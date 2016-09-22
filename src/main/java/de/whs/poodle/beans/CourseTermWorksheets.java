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

import de.whs.poodle.beans.evaluation.EvaluationWorksheet;
import de.whs.poodle.beans.mc.InstructorMcWorksheet;
import de.whs.poodle.beans.mc.StudentMcWorksheet;

/*
 * Wrapper for the worksheets in a specific course term.
 */
public class CourseTermWorksheets {

	private List<ExerciseWorksheet> exerciseWorksheets;
	private List<InstructorMcWorksheet> mcWorksheets;
	private List<StudentMcWorksheet> studentWorksheets;
	private EvaluationWorksheet evaluationWorksheet;

	public List<ExerciseWorksheet> getExerciseWorksheets() {
		return exerciseWorksheets;
	}

	public void setExerciseWorksheets(List<ExerciseWorksheet> exerciseWorksheets) {
		this.exerciseWorksheets = exerciseWorksheets;
	}

	public List<InstructorMcWorksheet> getMcWorksheets() {
		return mcWorksheets;
	}

	public void setMcWorksheets(List<InstructorMcWorksheet> mcWorksheets) {
		this.mcWorksheets = mcWorksheets;
	}

	public List<StudentMcWorksheet> getStudentWorksheets() {
		return studentWorksheets;
	}

	public void setStudentWorksheets(List<StudentMcWorksheet> studentWorksheets) {
		this.studentWorksheets = studentWorksheets;
	}

	public EvaluationWorksheet getEvaluationWorksheet() {
		return evaluationWorksheet;
	}

	public void setEvaluationWorksheet(EvaluationWorksheet evaluationWorksheet) {
		this.evaluationWorksheet = evaluationWorksheet;
	}

	public boolean isAllEmpty() {
		return exerciseWorksheets.isEmpty() && mcWorksheets.isEmpty()
				&& evaluationWorksheet == null;
	}
}
