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

import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import de.whs.poodle.Poodle;
import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.Student;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.CourseTermRepository;
import de.whs.poodle.repositories.InstructorRepository;
import de.whs.poodle.repositories.StudentRepository;

/*
 * This is the global controller, meaning the @ModelAttribute
 * functions in this class are called on _every_ request. We
 * use this to add some attributes to the model that we
 * need on all or most pages.
 */
@ControllerAdvice(basePackageClasses = Poodle.class) // basePackageClasses for https://github.com/spring-projects/spring-boot/issues/1940
public class GlobalControllerAdvice {

	@Autowired
	private CourseTermRepository courseTermRepo;

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private StudentRepository studentRepo;

	@Autowired
	private InstructorRepository instructorRepo;

	@ModelAttribute
	public void populateModel(Model model, Principal principal, HttpServletRequest request) {
		// various info about login etc.
		boolean isStudent = request.isUserInRole("ROLE_STUDENT");
		boolean isInstructor = request.isUserInRole("ROLE_INSTRUCTOR");
		boolean isAdmin = request.isUserInRole("ROLE_ADMIN");
		boolean isSwitched = request.isUserInRole("ROLE_PREVIOUS_ADMINISTRATOR");
		boolean isInStudentMode = request.isUserInRole("ROLE_FAKE_STUDENT");

		model.addAttribute("isStudent",isStudent);
		model.addAttribute("isInstructor", isInstructor);
		model.addAttribute("isAdmin", isAdmin);
		model.addAttribute("isSwitched", isSwitched);
		model.addAttribute("isInStudentMode", isInStudentMode);
		model.addAttribute("isLoggedIn", isStudent || isInstructor);

		if (isStudent) {
			// add the student object and the courseTerms he is enrolled to (for the navigation)
			Student student = studentRepo.getByUsername(principal.getName());
			List<CourseTerm> courseTerms = courseTermRepo.getEnrolledForStudent(student.getId());

			model.addAttribute("student", student);
			model.addAttribute("username", student.getUsername());
			model.addAttribute("globalCourseTerms", courseTerms);
		}
		else if (isInstructor) {
			//add the instructor object and his courses (for the navigation)
			Instructor instructor = instructorRepo.getByUsername(principal.getName());
			List<Course> course = courseRepo.getAllForInstructor(instructor.getId());

			model.addAttribute("instructor", instructor);
			model.addAttribute("username", instructor.getUsername());
			model.addAttribute("globalCourses", course);
		}
	}
}
