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
package de.whs.poodle.beans.forms;

import javax.validation.constraints.Max;

/*
 * Form used to create a StudentMcWorksheet (see MultipleChoiceController / multipleChoice.html).
 */
public class CreateMcWorksheetForm {

	public static final int MAX_QUESTIONS = 60;

	private boolean enableTagFilter;
	private int[] tags;

	@Max(value = MAX_QUESTIONS, message ="{maximumNQuestions}")
	private int maximum;

	private boolean ignoreAlreadyAnswered;

	public CreateMcWorksheetForm() {
		this.enableTagFilter = false;
		this.tags = new int[0];
		this.maximum = 20;
		this.ignoreAlreadyAnswered = false;
	}

	public boolean isEnableTagFilter() {
		return enableTagFilter;
	}

	public void setEnableTagFilter(boolean enableTagFilter) {
		this.enableTagFilter = enableTagFilter;
	}

	public int[] getTags() {
		if (enableTagFilter)
			return tags;
		else
			return new int[0];
	}

	public void setTags(int[] tags) {
		this.tags = tags;
	}

	public int getMaximum() {
		return maximum;
	}

	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}

	public boolean isIgnoreAlreadyAnswered() {
		return ignoreAlreadyAnswered;
	}

	public void setIgnoreAlreadyAnswered(boolean ignoreAlreadyAnswered) {
		this.ignoreAlreadyAnswered = ignoreAlreadyAnswered;
	}
}
