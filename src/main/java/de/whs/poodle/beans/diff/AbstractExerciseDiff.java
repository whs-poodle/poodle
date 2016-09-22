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

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import de.whs.poodle.beans.AbstractExercise;
import de.whs.poodle.beans.Tag;

/*
 * Base class for ExerciseDiff and McQuestionDiff.
 *
 * The text diffs are created with Google's diff-match-patch.
 *
 * https://code.google.com/p/google-diff-match-patch/
 */
public abstract class AbstractExerciseDiff {

	private List<TextDiff> textDiffList;

	// added/removed tags
	private ObjectListDiff<Tag> tagsDiff;

	private diff_match_patch dmp;

	public AbstractExerciseDiff(AbstractExercise exercise1, AbstractExercise exercise2) {
		this.dmp = new diff_match_patch();

		// exercise text
		String text1 = exercise1.getText();
		String text2 = exercise2.getText();
		this.textDiffList = diffText(text1, text2);

		this.tagsDiff = new ObjectListDiff<Tag>(exercise1.getTags(), exercise2.getTags());
	}

	/*
	 * returns the difference between two texts as a list
	 * of TextDiff objects.
	 */
	protected List<TextDiff> diffText(String text1, String text2) {
		if (text1 == null)
			text1 = "";
		if (text2 == null)
			text2 = "";

		LinkedList<Diff> diffList = dmp.diff_main(text1, text2);
		/* By default, the diff is completely character-based and
		 * may therefore be very hard to red. cleanupSemantic()
		 * merges successive diffs to make the diff easier
		 * to read. */
		dmp.diff_cleanupSemantic(diffList);

		// transform Diff objects to TextDiff objects (see TextDiff.java on why).
		return diffList.stream()
				.map(d -> new TextDiff(d))
				.collect(Collectors.toList());
	}

	// returns whether the passe diffList has any changes at all
	protected boolean hasChanges(List<TextDiff> diffList) {
		return diffList.stream()
				.anyMatch(d -> !d.isEqual());
	}

	public boolean isTextChanged() {
		return hasChanges(textDiffList);
	}

	public List<TextDiff> getTextDiffList() {
		return textDiffList;
	}

	public ObjectListDiff<Tag> getTagsDiff() {
		return tagsDiff;
	}
}
