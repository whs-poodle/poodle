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

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.CompletionStatus;
import de.whs.poodle.beans.ExerciseWorksheet;
import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.forms.FeedbackForm;
import de.whs.poodle.beans.statistics.Statistic;
import de.whs.poodle.beans.statistics.Statistic.StatisticSource;
import de.whs.poodle.repositories.ExerciseWorksheetRepository;
import de.whs.poodle.repositories.FeedbackRepository;
import de.whs.poodle.repositories.StatisticsRepository;

@Controller
@RequestMapping("student/worksheets/{worksheetId}")
public class StudentWorksheetController {

	@Autowired
	private ExerciseWorksheetRepository exerciseWorksheetRepo;

	@Autowired
	private FeedbackRepository feedbackRepo;

	@Autowired
	private StatisticsRepository statisticsRepo;

	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("@studentSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String get(@PathVariable int worksheetId, @ModelAttribute Student student, Model model) {
		ExerciseWorksheet worksheet = exerciseWorksheetRepo.getById(worksheetId);

		// map each exercise root id to its statistic (if the student has already given feedback)
		Map<Integer,Statistic> exerciseToStatisticMap = statisticsRepo.getExerciseToStatisticMapForWorksheet(student.getId(), worksheet.getId());

		model.addAttribute("worksheet", worksheet);
		model.addAttribute("exerciseToStatisticMap", exerciseToStatisticMap);
		model.addAttribute("completionStatusList", CompletionStatus.values());

		return "student/worksheet";
	}

	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("@studentSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String postFeedback(
			@PathVariable int worksheetId,
			@ModelAttribute Student student,
			@ModelAttribute FeedbackForm form,
			RedirectAttributes redirectAttributes) {

		int studentId = student.getId();
		int courseTermId = exerciseWorksheetRepo.getById(worksheetId).getCourseTerm().getId();

		feedbackRepo.saveFeedback(form, studentId, courseTermId, StatisticSource.EXERCISE_WORKSHEET);

		if (form.isEmpty())
			redirectAttributes.addFlashAttribute("okMessageCode", "exerciseMarkedAsCompleted");
		else
			redirectAttributes.addFlashAttribute("okMessageCode", "exerciseMarkedAsCompletedWithThanks");

		return "redirect:/student/worksheets/{worksheetId}";
	}
}
