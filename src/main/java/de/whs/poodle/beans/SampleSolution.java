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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/*
 * A sample solution for an exercise.
 *
 * A sample solution is either a text or a file, but never
 * both. See also Exercise.getSampleSolutionType().
 */

// this is not its own table, but part of the exercise table
@Embeddable
public class SampleSolution {

	@Column(name="sample_solution_text")
	private String text;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="sample_solution_file_id")
	private PoodleFile poodleFile;

	public SampleSolution() {}

	public SampleSolution(String text) {
		this.text = text;
	}

	public SampleSolution(PoodleFile poodleFile) {
		this.poodleFile = poodleFile;
	}

	public String getText() {
		return text;
	}

	public PoodleFile getFile() {
		return poodleFile;
	}

	public void setFile(PoodleFile poodleFile) {
		this.poodleFile = poodleFile;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((poodleFile == null) ? 0 : poodleFile.hashCode());
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
		SampleSolution other = (SampleSolution) obj;
		if (poodleFile == null) {
			if (other.poodleFile != null)
				return false;
		} else if (!poodleFile.equals(other.poodleFile))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}
}
