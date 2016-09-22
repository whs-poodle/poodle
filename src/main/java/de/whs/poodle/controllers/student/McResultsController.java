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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.mc.McWorksheet;
import de.whs.poodle.beans.mc.McWorksheetResults;
import de.whs.poodle.repositories.McStatisticsRepository;
import de.whs.poodle.repositories.McWorksheetRepository;

@Controller
@RequestMapping("/student/multipleChoiceResults/{mcWorksheetId}")
public class McResultsController {

	@Autowired
	private McWorksheetRepository mcWorksheetRepo;

	@Autowired
	private McStatisticsRepository mcStatisticsRepo;

	@RequestMapping
	@PreAuthorize("@studentSecurity.hasAccessToMcWorksheet(authentication.name, #mcWorksheetId)")
	public String get(
			@ModelAttribute Student student,
			@PathVariable int mcWorksheetId,
			Model model) {
		McWorksheet mcWorksheet = mcWorksheetRepo.getByMcWorksheetId(mcWorksheetId);
		McWorksheetResults ownResults = mcStatisticsRepo.getStudentMcWorksheetResults(mcWorksheet, student.getId());

		boolean canSetPublic = mcWorksheetRepo.canStudentSetMcWorksheetPublic(student.getId(), mcWorksheetId);

		model.addAttribute("worksheet", mcWorksheet);
		model.addAttribute("ownResults", ownResults);
		model.addAttribute("canSetPublic", canSetPublic);

		return "student/mcResults";
	}

	@RequestMapping(method = RequestMethod.POST, params="setPublic")
	@PreAuthorize("@studentSecurity.hasAccessToMcWorksheet(authentication.name, #mcWorksheetId)")
	public String makePublic(
			@ModelAttribute Student student,
			@PathVariable int mcWorksheetId,
			RedirectAttributes redirectAttributes) {
		mcWorksheetRepo.setWorksheetPublic(student.getId(), mcWorksheetId);
		redirectAttributes.addFlashAttribute("okMessageCode", "mcWorksheetMadePublic");
		return "redirect:/student/multipleChoiceResults/{mcWorksheetId}";
	}
}
