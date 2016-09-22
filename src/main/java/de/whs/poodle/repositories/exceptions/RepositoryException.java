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
package de.whs.poodle.repositories.exceptions;

/*
 * This is thrown mostly within the repository classes, e.g. if the supplied data
 * is invalid "course with this name already exists etc". This exception
 * is treated specially in PoodleErrorAttributes since the messageCode is resolved
 * and returned to the client via Ajax where it is shown to the user.
 */
public abstract class RepositoryException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private String messageCode;
	private Object[] messageCodeArgs;

	public RepositoryException() {}

	public RepositoryException(String messageCode, Object[] messageCodeArgs) {
		this.messageCode = messageCode;
		this.messageCodeArgs = messageCodeArgs;
	}

	public RepositoryException(String messageCode) {
		this(messageCode, null);
	}

	public String getMessageCode() {
		return messageCode;
	}

	public Object[] getMessageCodeArgs() {
		return messageCodeArgs;
	}
}
