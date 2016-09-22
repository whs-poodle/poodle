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

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.Utils;
import de.whs.poodle.beans.AbstractExercise;
import de.whs.poodle.beans.AbstractExercise.ExerciseType;
import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.Tag;
import de.whs.poodle.beans.mc.McQuestion;
import de.whs.poodle.beans.mc.McQuestion.Answer;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.McQuestionRepository;
import de.whs.poodle.repositories.TagRepository;
import de.whs.poodle.repositories.exceptions.BadRequestException;
import de.whs.poodle.repositories.exceptions.ForbiddenException;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Controller
public class McQuestionEditorController {

	@Autowired
	private TagRepository tagRepo;

	@Autowired
	private McQuestionRepository mcQuestionRepo;

	@Autowired
	private CourseRepository courseRepo;

	private void populateModel(Model model, int courseId) {
		Course course = courseRepo.getById(courseId);
		List<Tag> tags = tagRepo.getForCourse(courseId, ExerciseType.ALL);

		model.addAttribute("visibilities", AbstractExercise.Visibility.values());
		model.addAttribute("course", course);
		model.addAttribute("tags", tags);
	}

	@RequestMapping(value="/instructor/mcQuestions/new", method = RequestMethod.GET, params="courseId")
	public String get(@RequestParam int courseId, Model model) {
		McQuestion question = new McQuestion();
		question.setCourseId(courseId);

		populateModel(model, courseId);

		model.addAttribute("question", question);

		return "instructor/mcQuestionEditor";
	}

	@RequestMapping(value = "/instructor/mcQuestions/{questionId}/edit", method = RequestMethod.GET)
	@PreAuthorize("@instructorSecurity.hasAccessToMcQuestion(authentication.name, #questionId)")
	public String edit(@PathVariable int questionId, Model model) {
		McQuestion question = mcQuestionRepo.getById(questionId);
		if (question == null)
			throw new NotFoundException();

		model.addAttribute("question", question);

		populateModel(model, question.getCourseId());

		return "instructor/mcQuestionEditor";
	}

	@RequestMapping(value = "/instructor/mcQuestions/{questionId}/edit", method = RequestMethod.POST, params="delete")
	@PreAuthorize("@instructorSecurity.hasAccessToMcQuestion(authentication.name, #questionId)")
	public String delete(Model model, @PathVariable int questionId, RedirectAttributes redirectAttributes) {
		try {
			mcQuestionRepo.delete(questionId);
		} catch(ForbiddenException e) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "cantDeleteQuestion");
			return "redirect:/instructor/mcQuestions/{mcQuestionId}";
		}

		redirectAttributes.addFlashAttribute("okMessageCode", "questionDeleted");
		return "redirect:/instructor";
	}

	// called via JS to generate a new Answer-Input
	@RequestMapping(value = "/instructor/mcQuestions/getAnswerInput", method = RequestMethod.GET)
	public String getAnswerInput() {
		return "instructor/mcQuestionEditor :: answerInput";
	}

	@RequestMapping(value = "/instructor/mcQuestions/save", method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Integer> saveMcQuestion(
			@ModelAttribute McQuestion question,
			@ModelAttribute Instructor instructor,
			@RequestParam String[] answerTexts,
			@RequestParam(defaultValue = "-1") int correctSingle, // index of the only correct answer (multipleCorrectAnswers = false)
			@RequestParam(defaultValue = "") List<Integer> correctMultiple) // indexes of all correct answers (multipleCorrectAnswers = true)
			throws SQLException {

		question.setChangedBy(instructor);

		if (!question.isMultipleCorrectAnswers() && correctSingle == -1 ||
			question.isMultipleCorrectAnswers() && correctMultiple.isEmpty())
			throw new BadRequestException("noCorrectAnswerSelected");

		// avoid XSS
		question.setText(Utils.sanitizeHTML(question.getText()));

		// create the answer objects
		for (int i = 0; i < answerTexts.length; i++) {
			Answer a = new Answer();
			a.setText(answerTexts[i]);

			if (!question.isMultipleCorrectAnswers() && correctSingle == i ||
				question.isMultipleCorrectAnswers() && correctMultiple.contains(i))
				a.setCorrect(true);

			question.addAnswer(a);
		}

		int newId = mcQuestionRepo.save(question);

		// return the generated ID to the client
		return Collections.singletonMap("id", newId);
	}
}
