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

import java.util.List;

import de.whs.poodle.beans.CourseTerm;

/*
 * Interface used by InstructorMcWorksheet and StudentMcWorksheet.
 * Note that an abstract class would actually make more sense here,
 * but InstructorMcWorksheet already extends Worksheet and Java
 * doesn't allow multiple inheritance.
 */
public interface McWorksheet {

	public enum McWorksheetType {
		INSTRUCTOR, STUDENT
	}

	// ID of the worksheet (primary key)
	public int getId();

	// ID of the MC worksheet
	public int getMcWorksheetId();

	public CourseTerm getCourseTerm();
	public List<McQuestionOnWorksheet> getQuestions();
	public McWorksheetType getMcWorksheetType();

	// maximum number of points that a student can achieve on this worksheet
	default int getMaxPoints() {
		return getQuestions().stream()
				.mapToInt(qOnWs -> qOnWs.getQuestion().getMaxPoints())
				.sum();
	}
}
