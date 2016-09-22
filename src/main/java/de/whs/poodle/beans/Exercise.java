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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.AssociationOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name="v_exercise")
/* Exercise and McQuestion use difference tables for the relation to
 * the tags, so we have to define this here instead of the superclass. */
@AssociationOverride(
		name="tags",
		joinTable=@JoinTable(
				name="exercise_to_tag",
				joinColumns={@JoinColumn(name="exercise_id", referencedColumnName="id")},
				inverseJoinColumns={@JoinColumn(name="tag_id", referencedColumnName="id")}
		)
)
public class Exercise extends AbstractExercise implements Serializable {

	public enum SampleSolutionType {
		NONE, TEXT, FILE;
	}

	private static final long serialVersionUID = 1L;

	@Column(name="title")
	private String title;

	@ManyToMany
	@JoinTable(
			name="attachment",
			joinColumns={@JoinColumn(name="exercise_id", referencedColumnName="id")},
			inverseJoinColumns={@JoinColumn(name="file_id", referencedColumnName="id")})
	private List<PoodleFile> attachments;

	@Column(name="hint1")
	private String hint1;

	@Column(name="hint2")
	private String hint2;

	@Embedded
	private SampleSolution sampleSolution;

	public Exercise() {
		super();
		this.attachments = new ArrayList<>();
	}

	@Override
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getHint1() {
		return hint1;
	}

	public void setHint1(String hint1) {
		this.hint1 = hint1;
	}

	public String getHint2() {
		return hint2;
	}

	public void setHint2(String hint2) {
		this.hint2 = hint2;
	}

	public List<PoodleFile> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<PoodleFile> attachments) {
		this.attachments = attachments;
	}

	public SampleSolution getSampleSolution() {
		return sampleSolution;
	}

	public void setSampleSolution(SampleSolution sampleSolution) {
		this.sampleSolution = sampleSolution;
	}

	public boolean isHasHints() {
		return hint1 != null || hint2 != null || getSampleSolutionType() != SampleSolutionType.NONE;
	}

	public SampleSolutionType getSampleSolutionType() {
		if (sampleSolution == null)
			return SampleSolutionType.NONE;
		else if (sampleSolution.getText() != null)
			return SampleSolutionType.TEXT;
		else if( sampleSolution.getFile() != null)
			return SampleSolutionType.FILE;
		else
			return SampleSolutionType.NONE;
	}
}
