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
package de.whs.poodle.cli;

import java.io.Console;

/*
 * Various functions to get user input from the console. Used in
 * PoodleCliCommandLineRunner.
 */
public class ConsoleUtils {

	public static String getString(Console console, String fmt, Object... args) {
		String str = "";
		while (str.trim().isEmpty())
			str = console.readLine(fmt + ": ", args);

		return str;
	}

	public static String getPassword(Console console, String fmt, Object... args) {
		String str = "";
		while (str.trim().isEmpty())
			str = String.valueOf(console.readPassword(fmt + ": ", args));

		return str;
	}

	public static boolean getBoolean(Console console, String fmt, Object... args) {
		String str ="";
		Boolean b = null;

		do {
			str = console.readLine(fmt + " [y/n]: ", args);
			b = stringToBoolean(str);
		} while (b == null);

		return b;
	}

	private static Boolean stringToBoolean(String str) {
		if (str.equalsIgnoreCase("y") || str.equalsIgnoreCase("yes"))
			return true;

		if (str.equalsIgnoreCase("n") || str.equalsIgnoreCase("no"))
			return false;

		return null;
	}
}
