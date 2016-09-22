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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.InstructorRepository;
import de.whs.poodle.repositories.exceptions.BadRequestException;
import de.whs.poodle.repositories.exceptions.NotFoundException;
import de.whs.poodle.repositories.exceptions.RepositoryException;
import de.whs.poodle.services.AddTermToCourseService;

@Controller
@RequestMapping("instructor/courses/{courseId}")
public class ManageCourseController {

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private InstructorRepository instructorRepo;

	@Autowired
	private AddTermToCourseService addTermToCourseService;

	@ModelAttribute
	public void populateModel(@PathVariable int courseId, @ModelAttribute("globalCourses") List<Course> courses, Model model) {
		Course course = courseRepo.getById(courseId);
		if (course == null)
			throw new NotFoundException();

		/* list of instructors that could also have access to this course.
		 * We filter out the owner of the course since it doesn't make sense to display him. */
		List<Instructor> otherInstructors = instructorRepo.getAll();
		otherInstructors = otherInstructors.stream()
			.filter(i -> i.getId() != course.getInstructor().getId())
			.collect(Collectors.toList());

		/* List of courses that can be linked with this one. Filter out
		 * this course since it doesn't make sense to display it. */
		List<Course> linkedCourses = courses.stream()
				.filter(m -> m.getId() != courseId)
				.collect(Collectors.toList());

		model.addAttribute("course", course);
		model.addAttribute("otherInstructors", otherInstructors);
		model.addAttribute("linkedCourses", linkedCourses);
	}

	@RequestMapping
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String get(@PathVariable int courseId) {
		return "instructor/manageCourse";
	}

	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String edit(
			@PathVariable int courseId,
			@Valid Course course,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes,
			Model model) {
		if (bindingResult.hasErrors())
			return "instructor/manageCourse";

		try {
			course.setId(courseId);
			courseRepo.edit(course);
			redirectAttributes.addFlashAttribute("okMessageCode", "settingsSaved");
			return "redirect:/instructor/courses/{courseId}";
		} catch(RepositoryException e) {
			bindingResult.rejectValue("name", "courseWithThisNameAlreadyExists");
			return "instructor/manageCourse";
		}
	}

	@RequestMapping(method = RequestMethod.POST, params = "nextTermName")
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String addTerm(
			@ModelAttribute Instructor instructor,
			@PathVariable int courseId,
			@RequestParam String nextTermName,
			RedirectAttributes redirectAttributes) {
		try {
			addTermToCourseService.addTermToCourse(instructor.getId(), courseId, nextTermName);
			redirectAttributes.addFlashAttribute("okMessageCode", "termAdded");
		} catch(BadRequestException e) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "termAlreadyExistsForThisCourse");
		}

		return "redirect:/instructor/courses/{courseId}";
	}

	@RequestMapping(method = RequestMethod.POST, params="delete")
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String delete(@PathVariable int courseId, RedirectAttributes redirectAttributes) {
		courseRepo.delete(courseId);
		redirectAttributes.addFlashAttribute("okMessageCode", "courseDeleted");
		return "redirect:/instructor";
	}
}
