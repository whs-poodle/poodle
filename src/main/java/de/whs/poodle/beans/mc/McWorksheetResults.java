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

import java.util.Date;
import java.util.List;

import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.mc.McStatistic.Result;

/*
 * Gets a McWorksheet object and a list of corresponding
 * McStatistic objects and calculates the results for the
 * student and the worksheet (note that all McStatistic objects
 * must belong to the same student, anything else wouldn't make sense).
 */
public class McWorksheetResults {

	private McWorksheet mcWorksheet;
	private List<McStatistic> questionStatistics;

	public McWorksheetResults(McWorksheet mcWorksheet, List<McStatistic> questionStatistics) {
		this.mcWorksheet = mcWorksheet;
		this.questionStatistics = questionStatistics;
	}

	public McWorksheet getMcWorksheet() {
		return mcWorksheet;
	}

	public void setMcWorksheet(McWorksheet mcWorksheet) {
		this.mcWorksheet = mcWorksheet;
	}

	public List<McStatistic> getQuestionStatistics() {
		return questionStatistics;
	}

	public void setQuestionStatistics(List<McStatistic> questionStatistics) {
		this.questionStatistics = questionStatistics;
	}

	public int getPoints() {
		return questionStatistics.stream()
				.mapToInt(qs -> qs.getPoints())
				.sum();
	}

	public long getCorrectCount() {
		return questionStatistics.stream()
				.filter(qs -> qs.getResult() == Result.CORRECT)
				.count();
	}

	public long getWrongCount() {
		return questionStatistics.stream()
				.filter(qs -> qs.getResult() == Result.WRONG)
				.count();
	}

	public long getPartlyCorrectCount() {
		return questionStatistics.stream()
				.filter(qs -> qs.getResult() == Result.PARTLY)
				.count();
	}

	public int getQuestionCount() {
		return questionStatistics.size();
	}

	public boolean isCompleted() {
		return mcWorksheet.getQuestions().size() == questionStatistics.size();
	}

	public Date getCompletedAt() {
		/* use the maximum completedAt timestamp of the questions as
		 * the completedAt timestamp for the whole worksheet. */
		return questionStatistics.stream()
				.map(s -> s.getCompletedAt())
				.max((d1,d2) -> d1.compareTo(d2))
				.get();
	}

	/*
	 * This is the student who created these results which
	 * is not necessarily the one who created the worksheet.
	 * We can assume that all statistics are from the same
	 * student so just check who created the first one.
	 */
	public Student getStudent() {
		return questionStatistics.get(0).getStudent();
	}
}
