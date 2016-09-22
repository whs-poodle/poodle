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

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotBlank;

/*
 * Form used to send an email in EmailController / email.html.
 */
public class EmailForm {

	@Min(value = 1, message = "{noTermChosen}")
	private int courseTermId;

	@NotBlank(message = "{noSubjectSpecified}")
	private String subject;

	@NotBlank(message = "{noTextSpecified}")
	private String text;

	private boolean sendCopy;

	public int getCourseTermId() {
		return courseTermId;
	}

	public void setCourseTermId(int courseTermId) {
		this.courseTermId = courseTermId;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isSendCopy() {
		return sendCopy;
	}

	public void setSendCopy(boolean sendCopy) {
		this.sendCopy = sendCopy;
	}
}
