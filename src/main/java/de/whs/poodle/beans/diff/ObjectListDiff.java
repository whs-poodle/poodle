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

import java.util.ArrayList;
import java.util.List;

/*
 * Represents a diff between to two lists of a specific object type.
 *
 * For example, we pass the tag lists for the both exercises here
 * and automatically know which tags have been removed and which have
 * been added.
 */
public class ObjectListDiff<T> {

	private List<T> removed;
	private List<T> added;

	public ObjectListDiff(List<T> list1, List<T> list2) {
		this.added = new ArrayList<>(list2);
		this.added.removeAll(list1);
		this.removed = new ArrayList<>(list1);
		this.removed.removeAll(list2);
	}

	public List<T> getRemoved() {
		return this.removed;
	}

	public List<T> getAdded() {
		return this.added;
	}

	public boolean isChanged() {
		return !removed.isEmpty() || !added.isEmpty();
	}
}
