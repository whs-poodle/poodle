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
package de.whs.poodle.beans.evaluation;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import de.whs.poodle.beans.Worksheet;

@Entity
@Table(name="evaluation_worksheet")
public class EvaluationWorksheet extends Worksheet implements Serializable {

	private static final long serialVersionUID = 1L;

	@OneToMany
	@JoinColumn(name = "evaluation_worksheet_id")
	@OrderBy("number")
	private List<EvaluationSection> sections;

	@Column(name = "unlocked_until")
	private Date unlockedUntil;

	public List<EvaluationSection> getSections() {
		return sections;
	}

	public void setSections(List<EvaluationSection> sections) {
		this.sections = sections;
	}

	public Date getUnlockedUntil() {
		return unlockedUntil;
	}

	public void setUnlockedUntil(Date unlockedUntil) {
		this.unlockedUntil = unlockedUntil;
	}

	public int getQuestionCount() {
		return sections.stream()
				.mapToInt(s -> s.getQuestions().size())
				.sum();
	}

	public boolean isAccessibleForStudents(){
		return isUnlocked() && (unlockedUntil == null || unlockedUntil.after(new Date()));
	}
}
