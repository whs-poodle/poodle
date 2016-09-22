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

import java.util.List;
import java.util.stream.Collectors;

/*
 * Wraps a list of statistics and provides functions for
 * average values etc.
 */
public class StatisticList {

	private List<Statistic> list;

	private double avgFun;
	private double avgDifficulty;
	private int avgTime;
	private int feedbackCount;
	private int completedCount;

	public StatisticList(List<Statistic> list) {
		this.list = list;

		this.avgFun = list.stream()
				.filter(s -> !s.isIgnore() && s.getFun() != null)
				.mapToInt(s -> s.getFun())
				.average().orElse(0);

		this.avgDifficulty = list.stream()
				.filter(s -> !s.isIgnore() && s.getDifficulty() != null)
				.mapToInt(s -> s.getDifficulty())
				.average().orElse(0);

		double dAvgTime = list.stream()
				.filter(s -> !s.isIgnore() && s.getTime() != null)
				.mapToInt(s -> s.getTime())
				.average().orElse(0);

		// we round the average time since "10.3 minutes" isn't really useful
		this.avgTime = (int)Math.round(dAvgTime);

		this.feedbackCount = (int)list.stream()
				.filter(s -> !s.isEmpty())
				.count();

		this.completedCount = list.size();
	}

	public List<Statistic> getList() {
		return list;
	}

	public List<Statistic> getListWithoutEmpty() {
		return list.stream()
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
	}

	public List<Statistic> getListWithoutEmptyOrIgnored() {
		return list.stream()
				.filter(s -> !s.isEmpty() && !s.isIgnore())
				.collect(Collectors.toList());
	}

	public boolean isHasFeedback() {
		return feedbackCount > 0;
	}

	public double getAvgFun() {
		return avgFun;
	}

	public double getAvgDifficulty() {
		return avgDifficulty;
	}

	public int getAvgTime() {
		return avgTime;
	}

	public int getFeedbackCount() {
		return feedbackCount;
	}

	public int getCompletedCount() {
		return completedCount;
	}

	public String getAvgDifficultyStr() {
		return avgDifficulty == 0 ? "-" : String.format("%.1f", avgDifficulty);
	}

	public String getAvgFunStr() {
		return avgFun == 0 ? "-" : String.format("%.1f", avgFun);
	}

	public String getAvgTimeStr() {
		return avgTime == 0 ? "-" : String.valueOf(avgTime);
	}

	public String getAvgTimeStrWithMin() {
		return avgTime == 0 ? "-" : String.valueOf(avgTime) + "min";
	}

	public boolean isEnoughFeedbackForStudents() {
		return feedbackCount >= 3 && (avgDifficulty > 0 || avgTime > 0 || avgFun > 0);
	}
}
