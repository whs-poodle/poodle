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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/*
 * Represents a question on an McWorksheet which also
 * provides us with the number of the question within
 * the worksheet and the ID from the mc_worksheet_to_question
 * table.
 */
@Entity
@Table(name="mc_worksheet_to_question")
public class McQuestionOnWorksheet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(name="mc_worksheet_id")
	private int mcWorksheetId;

	@Column(name="number")
	private int number;

	@ManyToOne
	@JoinColumn(name="mc_question_id")
	private McQuestion question;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getMcWorksheetId() {
		return mcWorksheetId;
	}

	public void setMcWorksheetId(int mcWorksheetId) {
		this.mcWorksheetId = mcWorksheetId;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public McQuestion getQuestion() {
		return question;
	}

	public void setQuestion(McQuestion question) {
		this.question = question;
	}
}