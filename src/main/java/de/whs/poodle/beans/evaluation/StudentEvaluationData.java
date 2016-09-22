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

/*
 * Object that contains the data about an evaluation filled
 * out by a particular student, i.e. which answer he chose on which
 * question.
 */
public class StudentEvaluationData {

	// list of choices that the student made
	private List<StudentEvQuestionChoice> questionChoices;

	public List<StudentEvQuestionChoice> getQuestionChoices() {
		return questionChoices;
	}

	public void setQuestionChoices(List<StudentEvQuestionChoice> questionChoices) {
		this.questionChoices = questionChoices;
	}

	public static class StudentEvQuestionChoice {

		// question that this choice is for
		private int evaluationQuestionId;

		// id of the choice (null if n/a was chosen or the it is a text question)
		private Integer choiceId;

		// only set if it is a text question
		private String text;

		public int getEvaluationQuestionId() {
			return evaluationQuestionId;
		}

		public void setEvaluationQuestionId(int evaluationQuestionId) {
			this.evaluationQuestionId = evaluationQuestionId;
		}

		public Integer getChoiceId() {
			return choiceId;
		}

		public void setChoiceId(Integer choiceId) {
			this.choiceId = choiceId;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
