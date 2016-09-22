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
package de.whs.poodle.beans.statistics;

import java.util.HashMap;
import java.util.Map;

/*
 * Statistics for each exercise on a particular worksheet. These
 * are displayed in instructor/worksheet.html.
 * Note that the statistics for the exercises only contain
 * those for the particular CourseTerm the worksheet belongs to.
 */
public class WorksheetStatistics {

	private int enrolledCount;
	private Map<Integer,StatisticList> exerciseStatistics;

	public WorksheetStatistics() {
		this.exerciseStatistics = new HashMap<>();
	}

	public void addExerciseStatistic(int exerciseRootId, StatisticList sl) {
		exerciseStatistics.put(exerciseRootId, sl);
	}

	public StatisticList getStatisticFor(int exerciseRootId) {
		return exerciseStatistics.get(exerciseRootId);
	}

	public int getEnrolledCount() {
		return enrolledCount;
	}

	public void setEnrolledCount(int enrolledCount) {
		this.enrolledCount = enrolledCount;
	}

	public double getAvgFun() {
		return exerciseStatistics.values().stream()
				.mapToDouble(es -> es.getAvgFun())
				.filter(s -> s > 0)
				.average()
				.orElse(0);
	}

	public double getAvgDifficulty() {
		return exerciseStatistics.values().stream()
				.mapToDouble(as -> as.getAvgDifficulty())
				.filter(s -> s > 0)
				.average()
				.orElse(0);
	}

	public int getAvgTime() {
		return exerciseStatistics.values().stream()
				.mapToInt(as -> as.getAvgTime())
				.sum();
	}

	public String getAvgDifficultyStr() {
		double avgDifficulty = getAvgDifficulty();
		return avgDifficulty == 0 ? "-" : String.format("%.1f", avgDifficulty);
	}

	public String getAvgFunStr() {
		double avgFun = getAvgFun();
		return avgFun == 0 ? "-" : String.format("%.1f", avgFun);
	}

	public String getAvgTimeStr() {
		int avgTime = getAvgTime();
		return avgTime == 0 ? "-" : String.valueOf(avgTime) + "min";
	}
}
