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
package de.whs.poodle;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;

import de.whs.poodle.repositories.exceptions.RepositoryException;

/*
 * Overrides the DefaultErrorAttributes by Spring, so that Exceptions
 * are always logged and we only return the variables "status" and "message,
 *
 * Depending on whether the client makes an HTML or Ajax-Request, Spring automatically
 * either forwards to error.html or returns the variables as a JSON object.
 *
 * see also Spring's BasicErrorController and
 * http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-error-handling
 */
@Component
public class PoodleErrorAttributes extends DefaultErrorAttributes {

	private static Logger log = LoggerFactory.getLogger(PoodleErrorAttributes.class);

	@Autowired
	private MessageSource messageSource;

	@Override
	public Map<String, Object> getErrorAttributes(RequestAttributes requestAttributes, boolean includeStackTrace) {
		Map<String, Object> errorAttributes = new HashMap<>();

		Throwable error = getError(requestAttributes);

		String message = null;

		if (error != null) {
			/* RepositoryExceptions contain messageCodes for localization.
			 * If the error was a RepositoryException, create the corresponding
			 * text with the messageSource. We use this a lot with
			 * BadRequestException to localize responses to the client. */
			if (error instanceof RepositoryException) {
				RepositoryException daoExc = (RepositoryException)error;
				if (daoExc.getMessageCode() != null) {
					Locale locale = LocaleContextHolder.getLocale();
					message = messageSource.getMessage(daoExc.getMessageCode(), daoExc.getMessageCodeArgs(), locale);
				}
			}

			// show a more specific error on database connection failures
			if (error instanceof CannotGetJdbcConnectionException)
				message = "error connecting to database (" + error.getMessage() + ")";

			log.error("Error", error);
		}

		int status = (int)requestAttributes.getAttribute(
				RequestDispatcher.ERROR_STATUS_CODE, RequestAttributes.SCOPE_REQUEST);

		if (message == null)
			message = HttpStatus.valueOf(status).getReasonPhrase();

		errorAttributes.put("status", status);
		errorAttributes.put("message", message);

		return errorAttributes;
	}
}
