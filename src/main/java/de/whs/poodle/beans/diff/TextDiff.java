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

import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;

/*
 * A TextDiff object represents a single difference in a text. A diff over
 * a whole text is represent by a list of TextDiffs.
 *
 * For example, if the Text "aabbcc" is changed to "aaddcc", the list of
 * TextDiffs would look like the following:
 *
 * - EQUALS aa
 * - DELETE bb
 * - INSERT dd
 * - EQUALS cc
 *
 * This is more or less a wrapper for the "Diff" object in the
 * google-diff-match-patch library which doesn't comply with any
 * Java conventions and makes it impossible to use it in the View
 * layer (due to missing Getters).
 */
public class TextDiff {

	private Diff diff;

	public TextDiff(Diff diff) {
		this.diff = diff;
	}

	public String getText() {
		return diff.text;
	}

	public boolean isEqual() {
		return diff.operation == Operation.EQUAL;
	}

	public boolean isInsert() {
		return diff.operation == Operation.INSERT;
	}

	public boolean isDelete() {
		return diff.operation == Operation.DELETE;
	}
}
