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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.Student;
import de.whs.poodle.repositories.CourseTermRepository;
import de.whs.poodle.repositories.McWorksheetRepository;

@Controller
@RequestMapping("/student/courses")
public class CoursesController {

	@Autowired
	private CourseTermRepository courseTermRepo;

	@Autowired
	private McWorksheetRepository mcWorksheetRepo;

	@RequestMapping(method = RequestMethod.GET)
	public String get(@ModelAttribute Student student, Model model) {
		List<CourseTerm> enrolledCourseTerms = courseTermRepo.getEnrolledForStudent(student.getId());
		List<CourseTerm> availableCourseTerms = courseTermRepo.getNotEnrolledForStudent(student.getId());

		model.addAttribute("enrolledCourseTerms", enrolledCourseTerms);
		model.addAttribute("availableCourseTerms", availableCourseTerms);

		return "student/courses";
	}

	@RequestMapping(method = RequestMethod.POST, params="enroll")
	public String enroll(
			@ModelAttribute Student student,
			@RequestParam int courseTermId,
			@RequestParam(defaultValue = "") String password,
			RedirectAttributes redirectAttributes) {

		// check password, if the course has any
		CourseTerm courseTerm = courseTermRepo.getById(courseTermId);

		if (courseTerm.getCourse().isHasPassword() && !courseTerm.getCourse().getPassword().equals(password)) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "invalidPassword");
			return "redirect:/student/courses";
		}

		courseTermRepo.enrollStudent(student.getId(), courseTermId);
		redirectAttributes.addFlashAttribute("okMessageCode", "youAreNowEnrolled");
		redirectAttributes.addFlashAttribute("messageCodeParams", new Object[] {courseTerm.toString()} );

		return "redirect:/student/courses";
	}

	@RequestMapping(method = RequestMethod.POST, params="unenroll")
	public String unenroll(@ModelAttribute Student student, @RequestParam int courseTermId) {
		mcWorksheetRepo.cancelWorksheet(student.getId(), courseTermId);
		courseTermRepo.unenrollStudent(student.getId(), courseTermId);
		return "redirect:/student/courses";
	}
}
