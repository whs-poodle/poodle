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
package de.whs.poodle.controllers.student;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.StudentConfig;
import de.whs.poodle.repositories.StudentRepository;

@Controller
@RequestMapping("student/settings")
public class StudentSettingsController {

	@Autowired
	private StudentRepository studentRepo;

	@RequestMapping
	public String get(@ModelAttribute Student student, Model model) {
		StudentConfig config = student.getConfig();
		model.addAttribute("studentConfig", config);
		return "student/settings";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String save(
			@ModelAttribute Student student,
			@ModelAttribute StudentConfig config,
			RedirectAttributes redirectAttributes) {
		studentRepo.updateConfigForStudent(student.getId(), config);
		redirectAttributes.addFlashAttribute("okMessageCode", "settingsSaved");
		return "redirect:/student/settings";
	}
}
