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

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.mc.McQuestionOnWorksheet;
import de.whs.poodle.beans.mc.McWorksheet;
import de.whs.poodle.repositories.McWorksheetRepository;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Controller
@RequestMapping("student/mcWorksheets/{mcWorksheetId}")
public class StudentMcWorksheetController {

	@Autowired
	private McWorksheetRepository mcWorksheetRepo;

	@RequestMapping
	@PreAuthorize("@studentSecurity.hasAccessToMcWorksheet(authentication.name, #mcWorksheetId)")
	public String get(@ModelAttribute Student student, @PathVariable int mcWorksheetId, Model model) {
		// get the worksheet
		McWorksheet mcWorksheet = mcWorksheetRepo.getByMcWorksheetId(mcWorksheetId);
		if (mcWorksheet == null)
			throw new NotFoundException();

		// get the question that the student has to answer next
		Integer currentQuestionId = mcWorksheetRepo.getCurrentQuestionIdForStudentAndWorksheet(student.getId(), mcWorksheetId);

		// no next question, this must mean that the worksheets has been completed, show the results
		if (currentQuestionId == null) {
			return "redirect:/student/multipleChoiceResults/{mcWorksheetId}";
		}

		// get the question
		McQuestionOnWorksheet currentQuestion =
				mcWorksheet.getQuestions().stream()
				.filter(q -> q.getId() == currentQuestionId)
				.findFirst()
				.orElse(null);

		if (currentQuestion == null)
			throw new RuntimeException("the current question for this worksheet is not in the worksheet, this shouldn't be possible...");

		// shuffle the answers
		Collections.shuffle(currentQuestion.getQuestion().getAnswers());

		// make sure to set a timestamp for "seen_at"
		mcWorksheetRepo.prepareMcStatistics(student.getId(), mcWorksheet.getCourseTerm().getId(), currentQuestion.getId());

		model.addAttribute("worksheet", mcWorksheet);
		model.addAttribute("questionOnWs", currentQuestion);

		return "student/answerMcQuestion";
	}


	@RequestMapping(method = RequestMethod.POST, params="answer")
	@PreAuthorize("@studentSecurity.hasAccessToMcWorksheet(authentication.name, #mcWorksheetId)")
	public String answerQuestion(
			@ModelAttribute Student student,
			@PathVariable int mcWorksheetId,
			@RequestParam List<Integer> answers,
			Model model) {
		Integer currentQuestionId = mcWorksheetRepo.getCurrentQuestionIdForStudentAndWorksheet(student.getId(), mcWorksheetId);
		mcWorksheetRepo.answerQuestion(student.getId(), currentQuestionId, answers);

		// redirect to the same page, we will automatically show the next question
		return "redirect:/student/mcWorksheets/{mcWorksheetId}";

	}
}
