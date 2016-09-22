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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/*
 * Various functions.
 */
@Component
public class Utils {

	@Autowired
	private MessageSource messageSource;

	/*
	 * Removes <script> tags etc. from HTML markup to avoid XSS attacks.
	 * This is used on the CKEditor inputs (exercise text etc.).
	 */
	public static String sanitizeHTML(String html) {

		Whitelist whitelist = Whitelist.relaxed()
									   .addAttributes(":all", "style") // allow style tags everywhere
									   .addAttributes("span", "class") // necessary for CKEditor MathJax plugin
									   .addAttributes("table", "border", "align", "cellspacing", "cellpadding")
									   .preserveRelativeLinks(true); // preserve our relative links to images or linked exercises

		/*
		 * HACK: The jsoup whitelist only allows relative links in <img src=""> if a base URL is set
		 * because otherwise it can't check whether the protocol is allowed. We have no way
		 * to get the real base URL here, so just set a dummy.
		 */
		return Jsoup.clean(
				html,
				"https://dummydomain.com/",
				whitelist);
	}

	/*
	 * Converts an int array into a String of the form {1,2,3}.
	 * We use this to be able to pass the array as a parameter to
	 * a PostgreSQL function with JPA. JPA does not support setting
	 * an array as a parameter. As a workaround, we create this
	 * String from an array and then cast it to INT[] in the function
	 * call.
	 *
	 * see http://www.postgresql.org/docs/current/static/arrays.html
	 */
	public static String idsToPsqlArrayString(int[] ids) {
		String str = "{";

		for (int i = 0; i < ids.length; i++) {
			str += ids[i];
			if (i != ids.length - 1)
				str += ",";
		}

		str += "}";

		return str;
	}

	// return a map with a single "message" object, with message being read from the messageSource (used in some Ajax functions)
	public Map<String,Object> simpleMessage(String messageCode, Object[] messageArgs) {
		Locale locale = LocaleContextHolder.getLocale();
		String message = messageSource.getMessage(messageCode, messageArgs, locale);

		return Collections.singletonMap("message", message);
	}

	public Map<String,Object> simpleMessage(String messageCode) {
		return simpleMessage(messageCode, null);
	}
}
