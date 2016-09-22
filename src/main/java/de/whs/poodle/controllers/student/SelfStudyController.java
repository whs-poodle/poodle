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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.CompletionStatus;
import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.SelfStudyWorksheet;
import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.StudentToCourseTerm;
import de.whs.poodle.beans.forms.FeedbackForm;
import de.whs.poodle.beans.statistics.Statistic;
import de.whs.poodle.repositories.FeedbackRepository;
import de.whs.poodle.repositories.StatisticsRepository;
import de.whs.poodle.repositories.StudentToCourseTermRepository;

@Controller
@RequestMapping("/student/selfStudy/{courseTermId}")
public class SelfStudyController {

	@Autowired
	private StudentToCourseTermRepository studentToCourseTermRepo;

	@Autowired
	private FeedbackRepository feedbackRepo;

	@Autowired
	private StatisticsRepository statisticsRepo;

	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("@studentSecurity.hasAccessToCourseTerm(authentication.name, #courseTermId)")
	public String get(@ModelAttribute Student student, @PathVariable int courseTermId, Model model) {
		StudentToCourseTerm studentToCourseTerm = studentToCourseTermRepo.get(student.getId(), courseTermId);
		CourseTerm courseTerm = studentToCourseTerm.getCourseTerm();
		SelfStudyWorksheet selfStudyWorksheet = studentToCourseTerm.getSelfStudyWorksheet();

		Map<Integer,Statistic> exerciseToStatisticMap = statisticsRepo.getExerciseToStatisticMapForSelfStudy(student.getId(), courseTermId);

		model.addAttribute("worksheet", selfStudyWorksheet);
		model.addAttribute("courseTerm", courseTerm);
		model.addAttribute("completionStatusList", CompletionStatus.values());
		model.addAttribute("exerciseToStatisticMap", exerciseToStatisticMap);
		model.addAttribute("courseId", courseTerm.getCourse().getId());

		return "student/selfStudy";
	}

	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("@studentSecurity.hasAccessToCourseTerm(authentication.name, #courseTermId)")
	public String postFeedback(
			@PathVariable int courseTermId,
			@ModelAttribute Student student,
			@ModelAttribute FeedbackForm form,
			RedirectAttributes redirectAttributes) {
		int studentId = student.getId();

		feedbackRepo.saveFeedbackAndRemoveExerciseFromSelfStudy(form, studentId, courseTermId);

		if (form.isEmpty())
			redirectAttributes.addFlashAttribute("okMessageCode", "exerciseRemovedFromWorksheet");
		else
			redirectAttributes.addFlashAttribute("okMessageCode", "exerciseRemovedFromWorksheetWithThanks");

		return "redirect:/student/selfStudy/{courseTermId}";
	}

	@RequestMapping(method = RequestMethod.POST, params="removeExercise")
	@PreAuthorize("@studentSecurity.hasAccessToCourseTerm(authentication.name, #courseTermId)")
	public String removeExercise(
			@ModelAttribute Student student,
			@PathVariable int courseTermId,
			@RequestParam int exerciseId,
			RedirectAttributes redirectAttributes) {
		studentToCourseTermRepo.removeExerciseFromWorksheet(student.getId(), courseTermId, exerciseId);
		redirectAttributes.addFlashAttribute("okMessageCode", "exerciseRemovedFromWorksheet");
		return "redirect:/student/selfStudy/{courseTermId}";
	}
}
