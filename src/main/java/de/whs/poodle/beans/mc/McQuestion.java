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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AssociationOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.jsoup.Jsoup;

import de.whs.poodle.beans.AbstractExercise;

@Entity
@Table(name="v_mc_question")
/* Exercise and McQuestion use difference tables for the relation to
 * the tags, so we have to define this here instead of the superclass. */
@AssociationOverride(
		name="tags",
		joinTable=@JoinTable(
				name="mc_question_to_tag",
				joinColumns={@JoinColumn(name="mc_question_id", referencedColumnName="id")},
				inverseJoinColumns={@JoinColumn(name="tag_id", referencedColumnName="id")}
		)
)
public class McQuestion extends AbstractExercise {

	public static final int MAX_ANSWERS = 8;

	@Column(name="has_multiple_correct_answers")
	private boolean multipleCorrectAnswers;

	@OneToMany(mappedBy="mcQuestionId")
	private List<Answer> answers;

	public McQuestion() {
		super();
		this.multipleCorrectAnswers = false;
		this.answers = new ArrayList<>();
	}

	public boolean isMultipleCorrectAnswers() {
		return multipleCorrectAnswers;
	}

	public void setMultipleCorrectAnswers(boolean multipleCorrectAnswers) {
		this.multipleCorrectAnswers = multipleCorrectAnswers;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	public void setAnswers(List<Answer> answers) {
		this.answers = answers;
	}

	public void addAnswer(Answer answer) {
		this.answers.add(answer);
	}

	public String getPlainText() {
		return Jsoup.parse(getText()).text();
	}

	@Override
	public String getTitle() {
		return Jsoup.parse(getText()).text();
	}

	public int getMaxPoints() {
		return (int)answers.stream()
				.filter(a -> a.isCorrect())
				.count();
	}

	@Entity
	@Table(name="mc_question_to_answer")
	public static class Answer {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private int id;

		@Column(name="mc_question_id")
		private int mcQuestionId;

		@Column(name="correct")
		public boolean correct;

		@Column(name="text")
		public String text;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public boolean isCorrect() {
			return correct;
		}

		public void setCorrect(boolean correct) {
			this.correct = correct;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (correct ? 1231 : 1237);
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
			Answer other = (Answer) obj;
			if (correct != other.correct)
				return false;
			if (text == null) {
				if (other.text != null)
					return false;
			} else if (!text.equals(other.text))
				return false;
			return true;
		}
	}
}
