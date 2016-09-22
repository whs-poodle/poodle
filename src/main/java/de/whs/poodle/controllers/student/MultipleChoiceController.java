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
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.AbstractExercise.ExerciseType;
import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.Tag;
import de.whs.poodle.beans.forms.CreateMcWorksheetForm;
import de.whs.poodle.beans.mc.McStudentCourseTermStatistics;
import de.whs.poodle.beans.mc.McWorksheetResults;
import de.whs.poodle.repositories.CourseTermRepository;
import de.whs.poodle.repositories.McQuestionRepository;
import de.whs.poodle.repositories.McStatisticsRepository;
import de.whs.poodle.repositories.McWorksheetRepository;
import de.whs.poodle.repositories.TagRepository;

@Controller
@RequestMapping("student/multipleChoice/{courseTermId}")
public class MultipleChoiceController {

	@Autowired
	private TagRepository tagRepo;

	@Autowired
	private McQuestionRepository mcQuestionRepo;

	@Autowired
	private McWorksheetRepository mcWorksheetRepo;

	@Autowired
	private McStatisticsRepository mcStatisticsRepo;

	@Autowired
	private CourseTermRepository courseTermRepo;

	private void populateModel(Model model, int studentId, int courseTermId) {
		CourseTerm courseTerm = courseTermRepo.getById(courseTermId);
		List<Tag> tags = tagRepo.getForStudentInCourse(courseTerm.getCourse().getId(), ExerciseType.MC_QUESTION);
		List<Tag> distinctTags = tagRepo.getDistinctTags(tags);
		McStudentCourseTermStatistics mcStudentCourseTermStatistics = mcWorksheetRepo.getMcStudentStatistics(studentId, courseTermId);

		List<McWorksheetResults> latestWorksheetsResults = mcStatisticsRepo.getLatestPublicStudentMcWorksheetsResults(studentId, courseTermId);
		List<McWorksheetResults> ownWorksheetsResults = mcStatisticsRepo.getOwnStudentMcWorksheetsResults(studentId, courseTermId);

		model.addAttribute("tags", tags);
		model.addAttribute("distinctTags", distinctTags);
		model.addAttribute("course", courseTerm.getCourse());
		model.addAttribute("mcStudentCourseTermStatistics", mcStudentCourseTermStatistics);
		model.addAttribute("courseTermId", courseTermId);
		model.addAttribute("latestWorksheetsResults", latestWorksheetsResults);
		model.addAttribute("ownWorksheetsResults", ownWorksheetsResults);
	}

	@RequestMapping
	@PreAuthorize("@studentSecurity.hasAccessToCourseTerm(authentication.name, #courseTermId)")
	public String get(@ModelAttribute Student student, @PathVariable int courseTermId, Model model) {
		CourseTerm courseTerm = courseTermRepo.getById(courseTermId);
		model.addAttribute("course", courseTerm.getCourse());

		if (!mcQuestionRepo.hasCoursePublicMcQuestions(courseTerm.getCourse().getId())) {
			return "student/noMcQuestions";
		}

		/* If the student generated an mc worksheet that he hasn't completed yet
		 * we show a message on the top of the site. */
		Integer generatedMcWorksheetId = mcWorksheetRepo.getCurrentGeneratedMcWorksheetId(student.getId(), courseTermId);

		if (generatedMcWorksheetId != null)
			model.addAttribute("generatedMcWorksheetId", generatedMcWorksheetId);

		populateModel(model, student.getId(), courseTermId);

		model.addAttribute("createMcWorksheetForm", new CreateMcWorksheetForm());
		return "student/multipleChoice";
	}

	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("@studentSecurity.hasAccessToCourseTerm(authentication.name, #courseTermId)")
	public String createWorksheet(
			@ModelAttribute Student student,
			@Valid CreateMcWorksheetForm form,
			BindingResult bindingResult,
			@PathVariable int courseTermId,
			Model model) {

		populateModel(model, student.getId(), courseTermId);

		if (bindingResult.hasErrors()) {
			return "student/multipleChoice";
		}

		if (form.isEnableTagFilter() && form.getTags().length == 0) {
			model.addAttribute("errorMessageCode", "noTagsChosen");
			return "student/multipleChoice";
		}

		mcWorksheetRepo.cancelWorksheet(student.getId(), courseTermId);

		int mcWorksheetId = mcWorksheetRepo.createMcWorksheet(form, student.getId(), courseTermId);

		// shouldn't happen since the "create worksheet" button is disabled in this case
		if (mcWorksheetId == 0) {
			model.addAttribute("errorMessageCode", "notEnoughQuestions");
			return "student/multipleChoice";
		}

		return "redirect:/student/mcWorksheets/" + mcWorksheetId;
	}


	@RequestMapping(method = RequestMethod.POST, params="cancel")
	@PreAuthorize("@studentSecurity.hasAccessToCourseTerm(authentication.name, #courseTermId)")
	public String cancelMcWorksheet(
			@ModelAttribute Student student,
			@PathVariable int courseTermId,
			RedirectAttributes redirectAttributes) {

		mcWorksheetRepo.cancelWorksheet(student.getId(), courseTermId);

		redirectAttributes.addFlashAttribute("okMessageCode", "mcWorksheetCanceled");
		return "redirect:/student/multipleChoice/{courseTermId}";
	}

	@RequestMapping(method = RequestMethod.POST, params="mcWorksheetId")
	@PreAuthorize("@studentSecurity.hasAccessToCourseTerm(authentication.name, #courseTermId)")
	public String workOnWorksheet(
			@ModelAttribute Student student,
			@PathVariable int courseTermId,
			@RequestParam int mcWorksheetId) {
		mcWorksheetRepo.cancelWorksheet(student.getId(), courseTermId);
		mcWorksheetRepo.setWorksheetForStudent(student.getId(), courseTermId, mcWorksheetId);
		return "redirect:/student/mcWorksheets/" + mcWorksheetId;
	}

	@RequestMapping(value = "mcQuestionCount", method = RequestMethod.GET)
	@ResponseBody
	public Map<String,Integer> getMcQuestionsCount(
			@ModelAttribute Student student,
			@ModelAttribute CreateMcWorksheetForm form,
			@PathVariable int courseTermId) {
		int count = mcWorksheetRepo.getCountForMcWorksheet(form, student.getId(), courseTermId);
		return Collections.singletonMap("count", count);
	}
}
