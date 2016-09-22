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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.mc.McQuestion.Answer;

/*
 * A McStatistic is a statistic for a McQuestion on a worksheet answered
 * by a student (not simply a McQuestion!). In other words, A McStatistic
 * represents a single answered McQuestion and therefore also contains
 * the given answers etc.
 * This object is also used to calculate the results for a whole worksheet
 * (McWorksheetResults).
 */
@Entity
@Table(name="v_mc_statistic")
public class McStatistic {

	// see getResult
	 public enum Result {
		 CORRECT, PARTLY, WRONG;
	 }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToMany
	@JoinTable(
			name="mc_chosen_answer",
			joinColumns={@JoinColumn(name="mc_statistic_id", referencedColumnName="id")},
			inverseJoinColumns={@JoinColumn(name="mc_answer_id", referencedColumnName="id")})
	private List<Answer> answers; // answers given by the students (not all answers on the question)

	@Column(name="mc_worksheet_id")
	private int mcWorksheetId;

	@ManyToOne
	@JoinColumn(name="mc_worksheet_to_question_id")
	private McQuestionOnWorksheet questionOnWorksheet;

	@Column(name="seen_at")
	private Date seenAt;

	@Column(name="completed_at")
	private Date completedAt;

	@Column(name="mc_question_root_id")
	private int mcQuestionRootId;

	@ManyToOne
	@JoinColumn(name = "student_id")
	private Student student;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	// convenience function used if question only has one correct answer
	public Answer getAnswer() {
		return answers.get(0);
	}

	public void setAnswers(List<Answer> answers) {
		this.answers = answers;
	}

	public int getMcWorksheetId() {
		return mcWorksheetId;
	}

	public void setMcWorksheetId(int mcWorksheetId) {
		this.mcWorksheetId = mcWorksheetId;
	}

	public McQuestionOnWorksheet getQuestionOnWorksheet() {
		return questionOnWorksheet;
	}

	public void setQuestionOnWorksheet(McQuestionOnWorksheet questionOnWorksheet) {
		this.questionOnWorksheet = questionOnWorksheet;
	}

	public McQuestion getQuestion() {
		return questionOnWorksheet.getQuestion();
	}

	public long getTime() {
		return (completedAt.getTime() - seenAt.getTime()) / 1000;
	}

	public Date getSeenAt() {
		return seenAt;
	}

	public void setSeenAt(Date seenAt) {
		this.seenAt = seenAt;
	}

	public Date getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Date completedAt) {
		this.completedAt = completedAt;
	}

	public int getMcQuestionRootId() {
		return mcQuestionRootId;
	}

	public void setMcQuestionRootId(int mcQuestionRootId) {
		this.mcQuestionRootId = mcQuestionRootId;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	/*
	 * Calculates the points the student
	 * gets for this particular answered question.
	 */
	public int getPoints() {
		if (getQuestion().isMultipleCorrectAnswers()) {
			/* This question has multiple correct answers.
			 * Add 1 for every correct answer and subtract
			 * one for every wrong answer. */
			int points = 0;
			for (Answer a : answers) {
				if (a.isCorrect())
					points++;
				else
					points--;
			}

			// don't allow negative points
			return Math.max(0, points);
		}
		// this question has only one correct answer, so it can only be 1 or 0 points
		else {
			return getAnswer().isCorrect() ? 1 : 0;
		}
	}

	public Result getResult() {
		int points = getPoints();

		// maximal points -> Correct
		if (points == getQuestion().getMaxPoints())
			return Result.CORRECT;
		// question has multiple correct answers, but at least one answer was correct -> partly correct
		else if (getQuestion().isMultipleCorrectAnswers() && answers.stream().anyMatch(a -> a.isCorrect()))
			return Result.PARTLY;
		// completely wrong
		else
			return Result.WRONG;
	}
}
