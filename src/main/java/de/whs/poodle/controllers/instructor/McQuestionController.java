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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.mc.McQuestion;
import de.whs.poodle.beans.mc.McStatistic;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.McQuestionRepository;
import de.whs.poodle.repositories.McStatisticsRepository;
import de.whs.poodle.repositories.exceptions.ForbiddenException;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Controller
@RequestMapping("/instructor/mcQuestions/{mcQuestionId}")
public class McQuestionController {

	@Autowired
	private McQuestionRepository mcQuestionRepo;

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private McStatisticsRepository mcStatisticsRepo;

	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("@instructorSecurity.hasAccessToMcQuestion(authentication.name, #mcQuestionId)")
	public String get(@PathVariable int mcQuestionId, @RequestParam(required = false) boolean saveSuccess, Model model) {
		McQuestion question = mcQuestionRepo.getById(mcQuestionId);
		if (question == null)
			throw new NotFoundException();

		Course course = courseRepo.getById(question.getCourseId());
		List<McQuestion> revisions = mcQuestionRepo.getAllRevisionsForRoot(question.getRootId());
		List<McStatistic> statistics = mcStatisticsRepo.getForQuestion(question.getId());

		model.addAttribute("question", question);
		model.addAttribute("course", course);
		model.addAttribute("revisions", revisions);
		model.addAttribute("statistics", statistics);

		if (saveSuccess)
			model.addAttribute("okMessageCode", "questionSaved");

		return "instructor/mcQuestion";
	}

	@RequestMapping(method = RequestMethod.POST, params="delete")
	@PreAuthorize("@instructorSecurity.hasAccessToMcQuestion(authentication.name, #mcQuestionId)")
	public String delete(Model model, @PathVariable int mcQuestionId, RedirectAttributes redirectAttributes) {
		try {
			mcQuestionRepo.delete(mcQuestionId);
		} catch(ForbiddenException e) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "cantDeleteQuestion");
			return "redirect:/instructor/mcQuestion/{mcQuestionId}";
		}

		redirectAttributes.addFlashAttribute("okMessageCode", "questionDeleted");
		return "redirect:/instructor";
	}

	@RequestMapping(method = RequestMethod.GET, params={"oldId", "newId"})
	public String showDiff(@RequestParam int oldId, @RequestParam int newId) {
		return "redirect:/instructor/mcQuestionDiff/" + oldId + "/" + newId;
	}
}
