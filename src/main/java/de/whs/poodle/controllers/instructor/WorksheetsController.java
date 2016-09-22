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

import java.util.Date;
import java.util.LinkedHashMap;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.CourseTermWorksheets;
import de.whs.poodle.beans.Worksheet;
import de.whs.poodle.beans.Worksheet.WorksheetType;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.EvaluationWorksheetRepository;
import de.whs.poodle.repositories.WorksheetRepository;
import de.whs.poodle.repositories.exceptions.BadRequestException;
import de.whs.poodle.repositories.exceptions.NotFoundException;
import de.whs.poodle.services.WorksheetUnlockEmailService;

@Controller
@RequestMapping("/instructor/courses/{courseId}/worksheets")
public class WorksheetsController {

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private WorksheetRepository worksheetRepo;

	@Autowired
	private WorksheetUnlockEmailService worksheetUnlockEmailService;

	@Autowired
	private EvaluationWorksheetRepository evaluationWorksheetRepo;

	private static Logger log = LoggerFactory.getLogger(WorksheetsController.class);

	/*
	 * show all course terms and its worksheets.
	 */
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String get(@PathVariable int courseId, Model model) {
		Course course = courseRepo.getById(courseId);
		if (course == null)
			throw new NotFoundException();

		LinkedHashMap<CourseTerm, CourseTermWorksheets> worksheetMap = worksheetRepo.getForCourse(courseId);

		model.addAttribute("course", course);
		model.addAttribute("worksheetMap", worksheetMap);

		return "instructor/worksheets";
	}

	@RequestMapping(method = RequestMethod.POST, params="deleteWorksheetId")
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String delete(@PathVariable int courseId, @RequestParam int deleteWorksheetId) {
		worksheetRepo.delete(deleteWorksheetId);
		return "redirect:/instructor/courses/{courseId}/worksheets";
	}

	@RequestMapping(method = RequestMethod.POST, params="unlockWorksheetId")
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String unlock(
			@PathVariable int courseId,
			RedirectAttributes redirectAttributes,
			@RequestParam int unlockWorksheetId) {
		Worksheet worksheet = worksheetRepo.getById(unlockWorksheetId);

		try {
			worksheetUnlockEmailService.unlockWorksheetAndSendEmail(worksheet.getId());
			redirectAttributes.addFlashAttribute("okMessageCode", "worksheetUnlocked." + worksheet.getType().name());
		} catch(MailException|MessagingException e) {
			log.error("failed to send email", e);
			redirectAttributes.addFlashAttribute("errorMessageCode", "unlockEmailFailed." + worksheet.getType().name());
			redirectAttributes.addFlashAttribute("messageCodeParams", new Object[]{e.getMessage()});
		}

		return "redirect:/instructor/courses/{courseId}/worksheets";
	}

	@RequestMapping(method = RequestMethod.POST, params={"worksheetId", "moveUp"})
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String move(@PathVariable int courseId, @RequestParam int worksheetId, @RequestParam boolean moveUp) {
		worksheetRepo.move(worksheetId, moveUp);
		return "redirect:/instructor/courses/{courseId}/worksheets";
	}

	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String create(
			@PathVariable int courseId,
			@RequestParam String title,
			@RequestParam int courseTermId,
			@RequestParam WorksheetType type,
			RedirectAttributes redirectAttributes) {
		try {
			int id = worksheetRepo.create(title, courseTermId, type);
			redirectAttributes.addFlashAttribute("okMessageCode", "worksheetCreated." + type.name());
			if (type == WorksheetType.EXERCISE)
				return "redirect:/instructor/worksheets/" + id + "/edit";
			else
				return "redirect:/instructor/mcWorksheets/" + id + "/edit";
		} catch(BadRequestException e) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "noTitleSpecified");
			return "redirect:/instructor/courses/{courseId}/worksheets";
		}
	}

	 @RequestMapping(method = RequestMethod.POST, params={"worksheetId", "dateTime"})
	 @PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	 public String setUnlockAt(
			 @PathVariable int courseId,
			 @RequestParam int worksheetId,
			 @RequestParam Date dateTime) {
		 worksheetRepo.setUnlockAt(worksheetId, dateTime);
		 return "redirect:/instructor/courses/{courseId}/worksheets";
	 }

	 @RequestMapping(method = RequestMethod.POST, params={"evaluationWsId", "unlockAt", "unlockedUntil"})
	 @PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	 public String setUnlockedUntil(
			 @PathVariable int courseId,
			 @RequestParam int evaluationWsId,
			 @RequestParam Date unlockAt,
			 @RequestParam Date unlockedUntil) {
		 evaluationWorksheetRepo.setUnlockTimes(evaluationWsId, unlockAt, unlockedUntil);
		 return "redirect:/instructor/courses/{courseId}/worksheets";
	 }
}