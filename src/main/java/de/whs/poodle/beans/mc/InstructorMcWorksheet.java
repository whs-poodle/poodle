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

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import de.whs.poodle.beans.Worksheet;

/*
 * A MC worksheet created by an instructor. Note that this basically
 * has two super classes, Worksheet and McWorksheet (interface).
 * The ID of this object is from the super class Worksheet.
 * However, we often need the ID of the McWorksheet parent (getMcWorksheetId()).
 * It is important to differentiate between the two.
 */
@Entity
@Table(name="instructor_mc_worksheet")
public class InstructorMcWorksheet extends Worksheet implements McWorksheet,Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name="mc_worksheet_id")
	private int mcWorksheetId;

	@OneToMany
	@JoinColumn(name = "mc_worksheet_id", referencedColumnName = "mc_worksheet_id")
	@OrderBy("number")
	private List<McQuestionOnWorksheet> questions;

	@Override
	public int getMcWorksheetId() {
		return mcWorksheetId;
	}

	public void setMcWorksheetId(int mcWorksheetId) {
		this.mcWorksheetId = mcWorksheetId;
	}

	@Override
	public List<McQuestionOnWorksheet> getQuestions() {
		return questions;
	}

	public void setQuestions(List<McQuestionOnWorksheet> questions) {
		this.questions = questions;
	}

	@Override
	public McWorksheetType getMcWorksheetType() {
		return McWorksheetType.INSTRUCTOR;
	}
}
