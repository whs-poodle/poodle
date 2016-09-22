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
package de.whs.poodle.beans.diff;

import de.whs.poodle.beans.mc.McQuestion;
import de.whs.poodle.beans.mc.McQuestion.Answer;

/*
 * Represents a diff between two McQuestions (usually
 * two revisions of the same question).
 */
public class McQuestionDiff extends AbstractExerciseDiff {

	private ObjectListDiff<Answer> answersDiff;

	public McQuestionDiff(McQuestion question1, McQuestion question2) {
		super(question1, question2);

		this.answersDiff = new ObjectListDiff<>(question1.getAnswers(), question2.getAnswers());
	}

	public ObjectListDiff<Answer> getAnswersDiff() {
		return this.answersDiff;
	}
}
