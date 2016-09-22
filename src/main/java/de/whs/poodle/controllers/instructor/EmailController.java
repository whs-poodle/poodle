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

import javax.mail.MessagingException;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.forms.EmailForm;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.CourseTermRepository;
import de.whs.poodle.repositories.exceptions.NotFoundException;
import de.whs.poodle.services.EmailService;

@Controller
@RequestMapping("instructor/courses/{courseId}/email")
@ConditionalOnProperty("poodle.emailEnabled")
public class EmailController {

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private CourseTermRepository courseTermRepo;

	@Autowired
	private EmailService emailService;

	private static Logger log = LoggerFactory.getLogger(EmailController.class);

	@ModelAttribute
	public void populateModel(@PathVariable int courseId, Model model) {
		Course course = courseRepo.getById(courseId);
		if (course == null)
			throw new NotFoundException();

		List<CourseTerm> courseTerms = courseTermRepo.getForCourse(courseId);

		model.addAttribute("course", course);
		model.addAttribute("courseTerms", courseTerms);
	}

	@RequestMapping
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String get(EmailForm emailForm, @PathVariable int courseId) {
		return "instructor/email";
	}

	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String sendMail(
			@PathVariable int courseId,
			@Valid EmailForm emailForm,
			BindingResult bindingResult,
			@ModelAttribute Instructor instructor,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors())
			return "instructor/email";

		List<String> studentUsernames = courseTermRepo.getEmailMessageRecipients(emailForm.getCourseTermId());

		try {
			// send e-mail
			emailService.sendMail(instructor, null, studentUsernames, emailForm.isSendCopy(), emailForm.getSubject(), emailForm.getText(), true);
			redirectAttributes.addFlashAttribute("okMessageCode", "emailSent");
			return "redirect:/instructor/courses/{courseId}/email";
		} catch(MailException|MessagingException e) {
			log.error("failed to send email", e);
			model.addAttribute("errorMessageCode", "unknownMailError");
			model.addAttribute("messageCodeParams", new Object[]{e.getMessage()});
			return "instructor/email";
		}
	}
}
