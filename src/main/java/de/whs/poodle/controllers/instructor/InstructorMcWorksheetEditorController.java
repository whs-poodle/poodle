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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.mc.InstructorMcWorksheet;
import de.whs.poodle.repositories.InstructorMcWorksheetRepository;
import de.whs.poodle.repositories.WorksheetRepository;
import de.whs.poodle.repositories.exceptions.BadRequestException;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Controller
@RequestMapping("instructor/mcWorksheets/{worksheetId}/edit")
public class InstructorMcWorksheetEditorController {

	@Autowired
	private WorksheetRepository worksheetRepo;

	@Autowired
	private InstructorMcWorksheetRepository instructorMcWorksheetRepo;

	@RequestMapping
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String edit(@PathVariable int worksheetId, Model model, RedirectAttributes redirectAttributes) {
		InstructorMcWorksheet worksheet = instructorMcWorksheetRepo.getById(worksheetId);
		if (worksheet == null)
			throw new NotFoundException();

		if (worksheet.isUnlocked()) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "mcWorksheetAlreadyUnlocked");
			return "redirect:/instructor/mcWorksheets/{worksheetId}";
		}

		model.addAttribute("worksheet", worksheet);
		return "instructor/mcWorksheetEditor";
	}

	@RequestMapping(method = RequestMethod.POST, params="removeMcWorksheetToQuestionId")
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String removeQuestion(@PathVariable int worksheetId, @RequestParam int removeMcWorksheetToQuestionId) {
		instructorMcWorksheetRepo.removeQuestion(removeMcWorksheetToQuestionId);
		return "redirect:/instructor/mcWorksheets/{worksheetId}/edit";
	}

	@RequestMapping(method = RequestMethod.POST, params="worksheetTitle")
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String changeTitle(@PathVariable int worksheetId, @RequestParam String worksheetTitle, RedirectAttributes redirectAttributes) {
		try {
			worksheetRepo.changeTitle(worksheetId, worksheetTitle);
		} catch(BadRequestException e) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "noTitleSpecified");
		}
		return "redirect:/instructor/mcWorksheets/{worksheetId}/edit";
	}

	@RequestMapping(method = RequestMethod.POST, params={"mcWorksheetToQuestionId", "moveUp"})
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String moveQuestion(
			@PathVariable int worksheetId,
			@RequestParam int mcWorksheetToQuestionId,
			@RequestParam boolean moveUp) {
		instructorMcWorksheetRepo.moveQuestion(mcWorksheetToQuestionId, moveUp);
		return "redirect:/instructor/mcWorksheets/{worksheetId}/edit";
	}
}
