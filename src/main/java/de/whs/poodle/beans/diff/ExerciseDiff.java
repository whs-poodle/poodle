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

import java.util.List;

import de.whs.poodle.beans.Exercise;
import de.whs.poodle.beans.Exercise.SampleSolutionType;
import de.whs.poodle.beans.PoodleFile;
import de.whs.poodle.beans.SampleSolution;

/*
 * Represents a diff between to different exercises
 * (usually two revisions of the same exercise).
 */
public class ExerciseDiff extends AbstractExerciseDiff {

	private List<TextDiff> titleDiffList;

	private List<TextDiff> hint1DiffList;
	private List<TextDiff> hint2DiffList;

	private ObjectListDiff<PoodleFile> attachmentsDiff;

	private Exercise exercise1;
	private Exercise exercise2;

	private SampleSolution solution1;
	private SampleSolution solution2;
	private List<TextDiff> solutionTextDiffList;

	public enum SolutionDiffType {
		TEXT, FILE, NONE, MIXED
	}

	public ExerciseDiff(Exercise exercise1, Exercise exercise2) {
		super(exercise1, exercise2);
		this.exercise1 = exercise1;
		this.exercise2 = exercise2;

		this.titleDiffList = diffText(exercise1.getTitle(), exercise2.getTitle());

		this.hint1DiffList = diffText(exercise1.getHint1(), exercise2.getHint1());
		this.hint2DiffList = diffText(exercise1.getHint2(), exercise2.getHint2());

		this.attachmentsDiff = new ObjectListDiff<PoodleFile>(exercise1.getAttachments(), exercise2.getAttachments());

		this.solution1 = exercise1.getSampleSolution();
		this.solution2 = exercise2.getSampleSolution();

		// solutionTextDiffList stays null if it is no text diff (see getSolutionDiffType())
		if (getSolutionDiffType() == SolutionDiffType.TEXT) {
			String text1 = solution1 != null ? solution1.getText() : null;
			String text2 = solution2 != null ? solution2.getText() : null;
			this.solutionTextDiffList = diffText(text1, text2);
		}
	}

	public boolean isTitleChanged() {
		return hasChanges(titleDiffList);
	}

	public List<TextDiff> getTitleDiffList() {
		return titleDiffList;
	}

	public boolean isHint1Changed() {
		return hasChanges(hint1DiffList);
	}

	public boolean isHint2Changed() {
		return hasChanges(hint2DiffList);
	}

	public List<TextDiff> getHint1DiffList() {
		return hint1DiffList;
	}

	public List<TextDiff> getHint2DiffList() {
		return hint2DiffList;
	}

	public ObjectListDiff<PoodleFile> getAttachmentsDiff() {
		return attachmentsDiff;
	}

	/* Returns how the sample solutions in the exercises differ
	 * (either not at all (NONE), different texts (TEXT), different
	 * files (TEXT) or different types (MIXED)).
	 */
	public SolutionDiffType getSolutionDiffType() {
		SampleSolutionType type1 = exercise1.getSampleSolutionType();
		SampleSolutionType type2 = exercise2.getSampleSolutionType();

		// sample solutions are equal or don't exist in both
		if (type1 == type2 &&
			(type1 == SampleSolutionType.NONE || solution1.equals(solution2))) {
				return SolutionDiffType.NONE;
		}

		/* different between two texts (if one of the solutions is empty
		 * we also consider this "text" so we can show the text diff in tabs
		 * on the page like for texts and hints as well). */
		boolean isTextDiff =
				type1 == SampleSolutionType.TEXT && type2 == SampleSolutionType.TEXT ||
				type1 == SampleSolutionType.NONE && type2 == SampleSolutionType.TEXT ||
				type1 == SampleSolutionType.TEXT && type2 == SampleSolutionType.NONE;

		if (isTextDiff)
			return SolutionDiffType.TEXT;

		/* like for the texts, we also consider this a file diff if one of the solutions
		 * is empty so we can simply show ( file1 -> file2) on the page in all cases). */
		boolean isFileDiff =
				type1 == SampleSolutionType.FILE && type2 == SampleSolutionType.FILE ||
				type1 == SampleSolutionType.NONE && type2 == SampleSolutionType.FILE ||
				type1 == SampleSolutionType.FILE && type2 == SampleSolutionType.NONE;

		if (isFileDiff)
			return SolutionDiffType.FILE;

		// one must be file, the other text
		return SolutionDiffType.MIXED;
	}

	public List<TextDiff> getSampleSolutionTextDiff() {
		return solutionTextDiffList;
	}
}
