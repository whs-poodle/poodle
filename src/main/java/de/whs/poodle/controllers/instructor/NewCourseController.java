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
package de.whs.poodle.controllers.instructor;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.forms.NewCourseForm;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.InstructorRepository;
import de.whs.poodle.repositories.exceptions.RepositoryException;

@Controller
@RequestMapping("instructor/courses/new")
public class NewCourseController {

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private InstructorRepository instructorRepo;

	@ModelAttribute
	public void populateModel(@ModelAttribute Instructor instructor, @ModelAttribute("globalCourses") List<Course> linkedCourses, Model model) {
		/* List of other instructors that can have access to this course.
		 * Filter out the current instructor since he will be the owner anyway. */
		List<Instructor> otherInstructors = instructorRepo.getAll();
		otherInstructors = otherInstructors.stream()
				.filter(d -> d.getId() != instructor.getId())
				.collect(Collectors.toList());

		model.addAttribute("otherInstructors", otherInstructors);
		model.addAttribute("linkedCourses", linkedCourses);
	}

	@RequestMapping
	public String get(NewCourseForm newCourseForm, Model model) {
		return "instructor/newCourse";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String create(
			@ModelAttribute Instructor instructor,
			@Valid NewCourseForm form,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes,
			Model model) {
		if (bindingResult.hasErrors())
			return "instructor/newCourse";

		try {
			Course course = form;
			course.setInstructor(instructor);
			courseRepo.create(course, form.getFirstTermName());
			redirectAttributes.addFlashAttribute("okMessageCode", "courseCreated");
			return "redirect:/instructor/courses/" + course.getId();
		} catch(RepositoryException e) {
			bindingResult.rejectValue("name", "courseWithThisNameAlreadyExists");
			return "instructor/newCourse";
		}
	}
}
