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
package de.whs.poodle.controllers;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import de.whs.poodle.controllers.AboutController.Credit.License;

@Controller
@RequestMapping("about")
public class AboutController {

	private static List<Credit> credits = createCredits();

	private static List<Credit> createCredits() {
		License apache2 = new License("Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0.html");

		List<Credit> credits = Arrays.asList(
			new Credit("jQuery", "https://jquery.com/", new License("MIT", "https://raw.githubusercontent.com/jquery/jquery/master/LICENSE.txt")),
			new Credit("jQuery UI", "https://jqueryui.com/", new License("MIT", "https://raw.githubusercontent.com/jquery/jquery-ui/master/LICENSE.txt")),
			new Credit("moment.js", "http://momentjs.com/", new License("MIT", "https://raw.githubusercontent.com/moment/moment/develop/LICENSE")),
			new Credit("JSoup", "http://jsoup.org/", new License("MIT", "http://jsoup.org/license")),
			new Credit("Bootstrap", "http://getbootstrap.com/", new License("MIT", "https://raw.githubusercontent.com/twbs/bootstrap/master/LICENSE")),
			new Credit("Bootstrap DateTimePicker", "https://github.com/Eonasdan/bootstrap-datetimepicker/", new License("MIT", "https://github.com/Eonasdan/bootstrap-datetimepicker/blob/master/LICENSE")),
			new Credit("DataTables", "https://datatables.net/", new License("MIT", "https://raw.githubusercontent.com/DataTables/DataTables/master/license.txt")),
			new Credit("PostgreSQL", "http://www.postgresql.org/", new License("PostgreSQL License", "http://www.postgresql.org/about/licence/")),
			new Credit("Spring", "https://spring.io/", apache2),
			new Credit("Spring Boot", "http://projects.spring.io/spring-boot/", apache2),
			new Credit("Thymeleaf", "http://www.thymeleaf.org/", apache2),
			new Credit("Jackson", "https://github.com/FasterXML/jackson", apache2),
			new Credit("Gradle", "http://gradle.org/", apache2),
			new Credit("MathJax", "http://www.mathjax.org/", apache2),
			new Credit("Apache Tomcat", "https://tomcat.apache.org/", apache2),
			new Credit("google-diff-match-patch", "https://code.google.com/p/google-diff-match-patch/", apache2),
			new Credit("CKEditor", "http://ckeditor.com/", new License("GPL", "https://www.gnu.org/licenses/gpl-3.0.txt"))
		);

		// sort by name
		credits.sort((c1,c2) -> c1.getName().compareTo(c2.getName()));

		return credits;
	}

	@RequestMapping
	public String get(Model model) {
		model.addAttribute("credits", credits);
		return "about";
	}

	public static class Credit {

		private String name;
		private String url;
		private License license;

		public Credit(String name, String url, License license) {
			this.name = name;
			this.url = url;
			this.license = license;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public License getLicense() {
			return license;
		}

		public void setLicense(License license) {
			this.license = license;
		}

		public static class License {

			public License(String name, String url) {
				super();
				this.name = name;
				this.url = url;
			}

			private String name;
			private String url;

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getUrl() {
				return url;
			}

			public void setUrl(String url) {
				this.url = url;
			}
		}
	}
}
