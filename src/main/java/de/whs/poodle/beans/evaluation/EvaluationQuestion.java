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

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="evaluation_question")
public class EvaluationQuestion {

	public enum EvaluationQuestionType {
		CHOICE, TEXT
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne
	@JoinColumn(name="evaluation_section_id")
	private EvaluationSection section;

	@Column(name="number")
	private int number;

	@Column(name="text")
	private String text;

	@Column(name="allow_not_applicable")
	private boolean allowNotApplicable;

	@OneToMany
	@JoinColumn(name="evaluation_question_id")
	private List<EvaluationQuestionChoice> choices;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public EvaluationSection getSection() {
		return section;
	}

	public void setSection(EvaluationSection section) {
		this.section = section;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isAllowNotApplicable() {
		return allowNotApplicable;
	}

	public void setAllowNotApplicable(boolean allowNotApplicable) {
		this.allowNotApplicable = allowNotApplicable;
	}

	public List<EvaluationQuestionChoice> getChoices() {
		return choices;
	}

	public void setChoices(List<EvaluationQuestionChoice> evaluationQuestionChoices) {
		this.choices = evaluationQuestionChoices;
	}

	public boolean isHasValues() {
		return choices.get(0).getValue() != null;
	}

	public EvaluationQuestionType getType() {
		if (choices.isEmpty())
			return EvaluationQuestionType.TEXT;
		else
			return EvaluationQuestionType.CHOICE;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (allowNotApplicable ? 1231 : 1237);
		result = prime * result + ((choices == null) ? 0 : choices.hashCode());
		result = prime * result + id;
		result = prime * result + number;
		result = prime * result + ((section == null) ? 0 : section.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EvaluationQuestion other = (EvaluationQuestion) obj;
		if (allowNotApplicable != other.allowNotApplicable)
			return false;
		if (choices == null) {
			if (other.choices != null)
				return false;
		} else if (!choices.equals(other.choices))
			return false;
		if (id != other.id)
			return false;
		if (number != other.number)
			return false;
		if (section == null) {
			if (other.section != null)
				return false;
		} else if (!section.equals(other.section))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}
}
