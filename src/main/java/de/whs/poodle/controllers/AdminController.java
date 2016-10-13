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

import java.util.List;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.Exercise;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.repositories.AdminRepository;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.ExerciseRepository;
import de.whs.poodle.repositories.InstructorRepository;
import de.whs.poodle.repositories.UserRepository;
import de.whs.poodle.services.EmailService;

@Controller
@RequestMapping("adminmenu")
public class AdminController {

	@Autowired
	private ExerciseRepository exerciseRepo;

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private InstructorRepository instructorRepo;

	@Autowired
	private AdminRepository adminRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired(required = false)
	private EmailService emailService;

	@ModelAttribute
	public void populateModel(
			Model model) {

				List<Instructor> allInstructors = instructorRepo.getAll();
				List<Course> courses = courseRepo.getAll();

				model.addAttribute("allInstructors", allInstructors);
				model.addAttribute("courses", courses);
	}

	@RequestMapping
	@PreAuthorize("@instructorSecurity.hasAdminAccess(authentication.name)")
	public String get(
			Model model) {

				return "adminmenu";
	}

	@RequestMapping(method = RequestMethod.POST, params = "changeAdmins")
	@PreAuthorize("@instructorSecurity.hasAdminAccess(authentication.name)")
	public String changeAdmins(
			@RequestParam(required=false) List<Integer> instructorIds,
			RedirectAttributes redirectAttributes,
			Model model) {

				if (instructorIds == null) {
					redirectAttributes.addFlashAttribute("errorMessageCode", "noInstructor");
				} else {
					redirectAttributes.addFlashAttribute("okMessageCode", "settingsSaved");
					adminRepo.changeAdmins(instructorIds);
				}

				return "redirect:/adminmenu";
	}

	@RequestMapping(method = RequestMethod.POST, params = "searchExerciseById")
	@PreAuthorize("@instructorSecurity.hasAdminAccess(authentication.name)")
	public String searchExerciseById(
			@RequestParam int searchExerciseId,
			RedirectAttributes redirectAttributes,
			Model model) {

				Exercise exercise = exerciseRepo.getById(searchExerciseId);

				if (exercise == null) {
					redirectAttributes.addFlashAttribute("errorMessageCode", "noExercisesFound");
					return "redirect:/adminmenu";
				}

				List<Instructor> courseInstructors = adminRepo.getInstructorsForExercise(exercise);

				model.addAttribute("courseInstructors", courseInstructors);
				model.addAttribute("exercise", exercise);

				return "adminmenu";
	}

	@RequestMapping(method = RequestMethod.POST, params = {"changeExerciseOwner"})
	@PreAuthorize("@instructorSecurity.hasAdminAccess(authentication.name)")
	public String changeExerciseOwner(
			@RequestParam int newOwnerId,
			@RequestParam int exerciseId,
			RedirectAttributes redirectAttributes,
			Model model) {

				adminRepo.changeOwner(newOwnerId, exerciseId);

				redirectAttributes.addFlashAttribute("okMessageCode", "exerciseSaved");

				return "redirect:/adminmenu";
	}

	@RequestMapping(method = RequestMethod.POST, params = "getInstructorForCourseId")
	@PreAuthorize("@instructorSecurity.hasAdminAccess(authentication.name)")
	public String getInstructorForCourseId(
			@RequestParam int getInstructorForCourseId,
			Model model) {

				Course course = courseRepo.getById(getInstructorForCourseId);

				List<Instructor> instructorsCourse = instructorRepo.getAll();

				instructorsCourse = instructorsCourse.stream()
						.filter(i -> i.getId() != course.getInstructor().getId())
						.collect(Collectors.toList());

				List<Integer> instructorsCourseIds = course.getOtherInstructorsIds();

				model.addAttribute("courseId", getInstructorForCourseId);
				model.addAttribute("instructorsCourse", instructorsCourse);
				model.addAttribute("instructorsCourseIds", instructorsCourseIds);

				return "adminmenu";
	}

	@RequestMapping(method = RequestMethod.POST, params = "changeCourseInstructor")
	@PreAuthorize("@instructorSecurity.hasAdminAccess(authentication.name)")
	public String changeCourseInstructors(
			@RequestParam(required=false) List<Integer> value,
			@RequestParam int changeCourseInstructor,
			RedirectAttributes redirectAttributes,
			Model model) {

				if (value == null) {
					redirectAttributes.addFlashAttribute("errorMessageCode", "noInstructor");
				} else {
					redirectAttributes.addFlashAttribute("okMessageCode", "settingsSaved");
					adminRepo.changeCourseInstructors(value, changeCourseInstructor);
				}

				return "redirect:/adminmenu";
	}

	@RequestMapping(method = RequestMethod.POST, params = "email")
	@PreAuthorize("@instructorSecurity.hasAdminAccess(authentication.name)")
	@ConditionalOnProperty("poodle.emailEnabled")
	public String email(
			@ModelAttribute Instructor instructor,
			@RequestParam String subject,
			@RequestParam String text,
			@RequestParam(required=false) boolean reply,
			RedirectAttributes redirectAttributes,
			Model model) {

				List<String> bccUsernames = userRepo.getAllEmailRecipients();

				try {
					emailService.sendMail(instructor, null, bccUsernames, reply, subject, text, true);
					redirectAttributes.addFlashAttribute("okMessageCode", "emailSent");
					return "redirect:/adminmenu";
				} catch(MailException|MessagingException|NullPointerException e) {
					model.addAttribute("errorMessageCode", "unknownMailError");
					model.addAttribute("messageCodeParams", new Object[]{e.getMessage()});
					return "adminmenu";
				}
			}
}
