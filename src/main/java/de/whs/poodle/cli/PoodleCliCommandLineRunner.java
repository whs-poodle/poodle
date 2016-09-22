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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.PoodleUser;
import de.whs.poodle.beans.Student;
import de.whs.poodle.repositories.InstructorRepository;
import de.whs.poodle.repositories.StudentRepository;
import de.whs.poodle.repositories.UserRepository;

/*
 * Poodle CLI main code.
 *
 * This implements CommandLineRunner which makes sure that Spring
 * automatically calls run() with the command line arguments.
 *
 * Also note that this is only loaded if the profile "cli" is active
 * because otherwise the web app would run this code as well.
 */
@Component
@Profile("cli")
public class PoodleCliCommandLineRunner implements CommandLineRunner {

	@Autowired
	private InstructorRepository instructorRepo;

	@Autowired
	private StudentRepository studentRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/*
	 * Called by Spring. Parse the arguments and
	 * do magic.
	 */
	@Override
	public void run(String... args) throws Exception {
		Console console = System.console();
		if (console == null) {
			System.err.println("failed to get console");
			return;
		}

		if (args.length == 0) {
			showHelp();
			return;
		}

		argLoop:
		for (String arg : args) {
			switch (arg) {
			case "createInstructor":
				createInstructor(console);
				break argLoop;

			case "createStudent":
				createStudent(console);
				break argLoop;

			case "resetPassword":
				resetPassword(console);
				break argLoop;

			case "--help":
			case "-h":
				showHelp();
				break argLoop;

			default:
				System.err.println("unknown argument: " + arg);
				break argLoop;
			}
		}
	}

	private void createInstructor(Console console) {
		String username = ConsoleUtils.getString(console, "Username");
		String firstName = ConsoleUtils.getString(console, "First Name");
		String lastName = ConsoleUtils.getString(console, "Last Name");
		String password = ConsoleUtils.getPassword(console, "Password");
		boolean isAdmin = ConsoleUtils.getBoolean(console, "Admin?");

		Instructor instructor = new Instructor();
		instructor.setUsername(username);
		instructor.setFirstName(firstName);
		instructor.setLastName(lastName);
		instructor.setAdmin(isAdmin);
		instructor.setPasswordHash(passwordEncoder.encode(password));

		instructorRepo.create(instructor);
	}

	private void createStudent(Console console) {
		String username = ConsoleUtils.getString(console, "Username");
		String password = ConsoleUtils.getPassword(console, "Password");

		Student student = new Student();
		student.setUsername(username);
		student.setPasswordHash(passwordEncoder.encode(password));

		studentRepo.create(student);
	}

	private void resetPassword(Console console) {
		String username = ConsoleUtils.getString(console, "Username");
		String password = ConsoleUtils.getPassword(console, "New Password");

		PoodleUser user = userRepo.getByUsername(username);
		if (user == null) {
			System.err.println("User does not exist");
			return;
		}

		String passwordHash = passwordEncoder.encode(password);

		userRepo.setPasswordHash(user.getId(), passwordHash);
	}

	private void showHelp() {
		System.out.println(
			"Poodle Command line\n\n" +
			"Commands:\n\n" +
			"createInstructor	   Create a new instructor\n" +
			"createStudent		   Create a new student\n" +
			"resetPassword		   Reset the password for a user\n"
		);
	}
}
