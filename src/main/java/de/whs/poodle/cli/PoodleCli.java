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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import de.whs.poodle.WebCliCommonConfig;

/*
 * Startup class for the Poodle CLI. This is called by Poodle.java if the "cli" parameter
 * is passed to the application.
 * Since we want to use the existing Repositories etc. we have to run this as a full
 * Spring Boot application.
 *
 * After Spring has initialized, it will automatically run PoodleCliCommandLineRunner which
 * contains the actual code for the CLI.
 */
@Configuration
// disable auto configurations we don't need here to save some startup time
@EnableAutoConfiguration(exclude = {
		ThymeleafAutoConfiguration.class,
		WebMvcAutoConfiguration.class,
		SecurityAutoConfiguration.class,
		JacksonAutoConfiguration.class,
		MailSenderAutoConfiguration.class
		})
// only scan for components we actually need
@ComponentScan(basePackages={"de.whs.poodle.cli", "de.whs.poodle.repositories"})
// specify the package for the JPA Entities. We have to define this because it only scans the sub-packages of this class by default.
@EntityScan(basePackages="de.whs.poodle.beans")
@Import(WebCliCommonConfig.class)
public class PoodleCli {

	public static void run(String[] args) {
		SpringApplication app = new SpringApplication(PoodleCli.class);

		// don't try to start tomcat etc.
		app.setWebEnvironment(false);

		// make sure PoodleCliCommandLineRunner and application-cli.properties are loaded
		app.setAdditionalProfiles("cli");

		/* Run the app. Spring will initialize everything, run the
		 * PoodleCliCommandLineRunner and then realize there is nothing
		 * else to do and exit automatically. */
		app.run(args);
	}
}
