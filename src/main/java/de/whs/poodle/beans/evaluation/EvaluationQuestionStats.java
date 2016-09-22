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

import java.util.ArrayList;
import java.util.List;

/*
 * Statistics for a single question on an evaluation worksheet.
 */
public class EvaluationQuestionStats {

	private List<ChoiceStats> choiceStats;
	private Double averageValue;

	public EvaluationQuestionStats(EvaluationQuestion question, List<EvaluationStatistic> stats) {
		this.choiceStats = new ArrayList<>();

		// iterate the choices
		for (EvaluationQuestionChoice choice : question.getChoices()) {
			// count how many times it was chosen
			int count = (int)
					stats.stream()
					.filter(s -> s.getChoice() != null && s.getChoice().getId() == choice.getId())
					.count();

			choiceStats.add(new ChoiceStats(choice, count));
		}

		/* Average value for the chosen choices. Note that this doesn't
		 * make sense if the choices don't have any value. It will
		 * just stay null in this case. */
		double avg = stats.stream()
				.filter(s -> s.getChoice() != null && s.getChoice().getValue() != null)
				.mapToInt(s -> s.getChoice().getValue())
				.average()
				.orElse(0);

		if (avg != 0)
			averageValue = avg;
	}

	public EvaluationQuestionStats() {}

	public List<ChoiceStats> getChoiceStats() {
		return choiceStats;
	}

	public void setChoiceStats(List<ChoiceStats> choiceStats) {
		this.choiceStats = choiceStats;
	}

	public void setAverageValue(Double averageValue) {
		this.averageValue = averageValue;
	}

	public Double getAverageValue() {
		return averageValue;
	}

	/*
	 * Defines how many times a choice of the question has been chosen.
	 */
	public static class ChoiceStats {

		private EvaluationQuestionChoice choice;
		private int count;

		public ChoiceStats(EvaluationQuestionChoice choice, int count) {
			this.choice = choice;
			this.count = count;
		}

		public EvaluationQuestionChoice getChoice() {
			return choice;
		}

		public void setChoice(EvaluationQuestionChoice choice) {
			this.choice = choice;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}
	}
}
