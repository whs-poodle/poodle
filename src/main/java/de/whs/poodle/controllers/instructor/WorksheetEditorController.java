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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.ExerciseWorksheet;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.repositories.ExerciseWorksheetRepository;
import de.whs.poodle.repositories.WorksheetRepository;
import de.whs.poodle.repositories.exceptions.BadRequestException;

@Controller
@RequestMapping("/instructor/worksheets/{worksheetId}/edit")
public class WorksheetEditorController {

	@Autowired
	private WorksheetRepository worksheetRepo;

	@Autowired
	private ExerciseWorksheetRepository exerciseWorksheetRepo;

	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String edit(@ModelAttribute Instructor instructor, @PathVariable int worksheetId, Model model, RedirectAttributes redirectAttributes) {
		ExerciseWorksheet worksheet = exerciseWorksheetRepo.getById(worksheetId);

		// unlocked worksheets can't be edited, redirect...
		if (worksheet.isUnlocked()) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "exerciseWorksheetAlreadyUnlocked");
			return "redirect:/instructor/worksheets/{worksheetId}";
		}

		model.addAttribute("worksheet", worksheet);

		return "instructor/worksheetEditor";
	}

	@RequestMapping(method = RequestMethod.POST, params="worksheetTitle")
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String changeTitle(@PathVariable int worksheetId, @RequestParam String worksheetTitle, RedirectAttributes redirectAttributes) {
		try {
			worksheetRepo.changeTitle(worksheetId, worksheetTitle);
		} catch(BadRequestException e) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "noTitleSpecified");
		}
		return "redirect:/instructor/worksheets/{worksheetId}/edit";
	}

	@RequestMapping(method = RequestMethod.POST, params="newChapterTitle")
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String addChapter(@PathVariable int worksheetId, @RequestParam String newChapterTitle, RedirectAttributes redirectAttributes) {
		try {
			exerciseWorksheetRepo.addChapter(worksheetId, newChapterTitle);
		} catch(BadRequestException e) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "noTitleSpecified");
		}
		return "redirect:/instructor/worksheets/{worksheetId}/edit";
	}

	@RequestMapping(method = RequestMethod.POST, params={"removeExerciseId", "chapterId"})
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String removeExerciseFromChapter(
			@PathVariable int worksheetId,
			@RequestParam int removeExerciseId,
			@RequestParam int chapterId) {
		exerciseWorksheetRepo.removeExerciseFromChapter(removeExerciseId, chapterId);
		return "redirect:/instructor/worksheets/{worksheetId}/edit";
	}

	@RequestMapping(method = RequestMethod.POST, params={"chapterId", "exerciseId", "moveUp"})
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String moveExercise(@PathVariable int worksheetId,
							   @RequestParam int chapterId,
							   @RequestParam int exerciseId,
							   @RequestParam boolean moveUp) {
		exerciseWorksheetRepo.moveExercise(chapterId, exerciseId, moveUp);
		return "redirect:/instructor/worksheets/{worksheetId}/edit";
	}

	@RequestMapping(method = RequestMethod.POST, params="removeChapterId")
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String removeChapter(@PathVariable int worksheetId, @RequestParam int removeChapterId) {
		exerciseWorksheetRepo.removeChapter(removeChapterId);
		return "redirect:/instructor/worksheets/{worksheetId}/edit";
	}

	@RequestMapping(method = RequestMethod.POST, params={"renameChapterId", "chapterTitle"})
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String renameChapter(@PathVariable int worksheetId, @RequestParam int renameChapterId, @RequestParam String chapterTitle, RedirectAttributes redirectAttributes) {
		try {
			exerciseWorksheetRepo.renameChapter(renameChapterId, chapterTitle);
		} catch(BadRequestException e) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "noTitleSpecified");
		}
		return "redirect:/instructor/worksheets/{worksheetId}/edit";
	}

	@RequestMapping(method = RequestMethod.POST, params={"moveUp", "chapterId"})
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String moveChapter(@PathVariable int worksheetId,
							  @RequestParam int chapterId,
							  @RequestParam boolean moveUp) {
		exerciseWorksheetRepo.moveChapter(chapterId, moveUp);
		return "redirect:/instructor/worksheets/{worksheetId}/edit";
	}
}
