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
package de.whs.poodle.beans;

import javax.persistence.AssociationOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;

/*
 * A Search Result for an Exercise. This contains some additional
 * statistic related attributes from v_exercise_search_result.
 *
 * This doesn't extend Exercise because this would cause JPA to query
 * this table every time an Exercise object is queried (because it has
 * to check whether the Exercise is actually an ExerciseSearchResult child
 * object, which doesn't make sense).
 *
 * This doesn't contain all attributes from Exercise because some of them
 * are simply not needed for the search function (hint1 etc.).
 *
 */
@Entity
@Table(name="v_exercise_search_result")
@AssociationOverride(
		name="tags",
		joinTable=@JoinTable(
				name="exercise_to_tag",
				joinColumns={@JoinColumn(name="exercise_id", referencedColumnName="id")},
				inverseJoinColumns={@JoinColumn(name="tag_id", referencedColumnName="id")}
		)
)
public class ExerciseSearchResult extends AbstractExercise {

	@Column(name="title")
	private String title;

	@Column(name="has_feedback", insertable=false)
	private boolean hasFeedback;

	@Column(name="avg_fun", insertable=false)
	private Double avgFun;

	@Column(name="avg_difficulty", insertable=false)
	private Double avgDifficulty;

	@Column(name="avg_time", insertable=false)
	private Integer avgTime;

	@Column(name="feedback_count", insertable=false)
	private int feedbackCount;

	@Column(name="completed_count", insertable=false)
	private int completedCount;

	@Override
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isHasFeedback() {
		return hasFeedback;
	}

	public void setHasFeedback(boolean hasFeedback) {
		this.hasFeedback = hasFeedback;
	}

	public Double getAvgFun() {
		return avgFun;
	}

	public void setAvgFun(Double avgFun) {
		this.avgFun = avgFun;
	}

	public Double getAvgDifficulty() {
		return avgDifficulty;
	}

	public void setAvgDifficulty(Double avgDifficulty) {
		this.avgDifficulty = avgDifficulty;
	}

	public long getAvgDifficultyRounded() {
		return Math.round(avgDifficulty);
	}

	public Integer getAvgTime() {
		return avgTime;
	}

	public void setAvgTime(Integer avgTime) {
		this.avgTime = avgTime;
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

	public int getFeedbackCount() {
		return feedbackCount;
	}

	public void setFeedbackCount(int feedbackCount) {
		this.feedbackCount = feedbackCount;
	}

	public int getCompletedCount() {
		return completedCount;
	}

	public void setCompletedCount(int completedCount) {
		this.completedCount = completedCount;
	}

	/* defines, whether a student is shown the feedback for this exercise in the search
	 * results for the self study worksheet. */
	public boolean isEnoughFeedbackForStudents() {
		return feedbackCount >= 3 && (avgDifficulty != null || avgTime != null || avgFun != null);
	}
}
