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
package de.whs.poodle.formatters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.format.Formatter;
import org.springframework.stereotype.Component;

/*
 * Default date formatter that is automatically used
 * by Thymeleaf when we print a date via the ${{...}} operator.
 * Formats the string with the format specified by the current
 * locale (dateTimeFormat code in messages_*.properties).
 *
 * This is also used to automatically parse dates for RequestParams, e.g.
 * dates by the Bootstrap DateTimePicker.
 */
@Component
public class DateFormatter implements Formatter<Date> {

	@Autowired
	private MessageSource messageSource;

	@Override
	public String print(Date date, Locale locale) {
		SimpleDateFormat dateFormat = createDateFormat(locale);
		return dateFormat.format(date);
	}

	@Override
	public Date parse(String text, Locale locale) throws ParseException {
		SimpleDateFormat dateFormat = createDateFormat(locale);
		return dateFormat.parse(text);
	}

	private SimpleDateFormat createDateFormat(final Locale locale) {
		String format = this.messageSource.getMessage("dateTimeFormat", null, locale);
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat;
	}
}
