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

import javax.servlet.http.HttpSession;

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
import de.whs.poodle.beans.Exercise;
import de.whs.poodle.beans.Exercise.SampleSolutionType;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.PoodleFile;
import de.whs.poodle.beans.SampleSolution;
import de.whs.poodle.beans.Tag;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.ExerciseRepository;
import de.whs.poodle.repositories.TagRepository;
import de.whs.poodle.repositories.exceptions.ForbiddenException;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Controller
public class ExerciseEditorController {

	@Autowired
	private ExerciseRepository exerciseRepo;

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private TagRepository tagRepo;

	private void populateModel(Model model, int courseId) {
		Course course = courseRepo.getById(courseId);
		List<Tag> tags = tagRepo.getForCourse(courseId, ExerciseType.ALL);

		model.addAttribute("course", course);
		model.addAttribute("tags", tags);
		model.addAttribute("visibilities", AbstractExercise.Visibility.values());
		model.addAttribute("sampleSolutionTypes", SampleSolutionType.values());
	}

	@RequestMapping(value="/instructor/exercises/new", method = RequestMethod.GET, params="courseId")
	public String get(@RequestParam int courseId, HttpSession session, Model model) {
		populateModel(model, courseId);
		Exercise exercise = new Exercise();
		exercise.setCourseId(courseId);

		exercise.setSampleSolution(new SampleSolution());

		model.addAttribute("exercise", exercise);

		return "instructor/exerciseEditor";
	}

	@RequestMapping(value = "/instructor/exercises/{exerciseId}/edit", method = RequestMethod.GET)
	@PreAuthorize("@instructorSecurity.hasAccessToExercise(authentication.name, #exerciseId)")
	public String edit(@PathVariable int exerciseId, Model model) {
		Exercise exercise = exerciseRepo.getById(exerciseId);
		if (exercise == null)
			throw new NotFoundException();

		// Every exercise must have a sampleSolution to be editable.
		// If the sampleSolution is equal to "null" the parser on the html
		// site will crash while parsing the th:text attribute in the textarea.
		if (exercise.getSampleSolution() == null){
			SampleSolution sampleSolution = new SampleSolution();
			exercise.setSampleSolution(sampleSolution);
		}

		if (!exercise.isLatestRevision()) {
			// old revisions can not be edited, redirect
			return "redirect:/instructor/exercises/{exerciseId}";
		}

		populateModel(model, exercise.getCourseId());

		model.addAttribute("exercise", exercise);

		return "instructor/exerciseEditor";
	}

	@RequestMapping(value = "/instructor/exercises/{exerciseId}/edit", method = RequestMethod.POST, params="delete")
	@PreAuthorize("@instructorSecurity.hasAccessToExercise(authentication.name, #exerciseId)")
	public String delete(Model model, @PathVariable int exerciseId, RedirectAttributes redirectAttributes) {
		try {
			exerciseRepo.delete(exerciseId);
		} catch(ForbiddenException e) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "cantDeleteExercise");
			return "redirect:/instructor/exercises/{exerciseId}/edit";
		}

		redirectAttributes.addFlashAttribute("okMessageCode", "exerciseDeleted");
		return "redirect:/instructor";
	}


	@RequestMapping(value = "/instructor/exercises/save", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Integer> saveExercise(
			@ModelAttribute Instructor instructor,
			@ModelAttribute Exercise exercise,
			@RequestParam SampleSolutionType sampleSolutionType,
			@RequestParam(defaultValue = "0") int sampleSolutionFileId,
			@RequestParam(required = false) List<Integer> attachmentIds,
			HttpSession session)
			throws SQLException {

		exercise.setChangedBy(instructor);

		// avoid XSS via CKEditor input
		exercise.setText(Utils.sanitizeHTML(exercise.getText()));
		exercise.setHint1(Utils.sanitizeHTML(exercise.getHint1()));
		exercise.setHint2(Utils.sanitizeHTML(exercise.getHint2()));

		// the hints stay null if they are empty
		if (exercise.getHint1().trim().isEmpty())
			exercise.setHint1(null);

		if (exercise.getHint2().trim().isEmpty())
			exercise.setHint2(null);

		// sample solution
		if (sampleSolutionType == SampleSolutionType.TEXT) {
			// avoid XSS
			String text = exercise.getSampleSolution().getText();
			exercise.getSampleSolution().setText(Utils.sanitizeHTML(text));
		}
		else if (sampleSolutionType == SampleSolutionType.FILE) {
			/* For the files, saveExercise() only needs the ID, so let's
			 * avoid another DB query. */
			if (sampleSolutionFileId != 0)
				exercise.setSampleSolution(new SampleSolution(new PoodleFile(sampleSolutionFileId)));
			else // type "file" chosen but no file specified, lets assume type "none"
				exercise.setSampleSolution(null);
		}
		else {
			exercise.setSampleSolution(null);
		}

		// attachments
		if (attachmentIds != null) {
		for (Integer id : attachmentIds)
			exercise.getAttachments().add(new PoodleFile(id));
		}

		// exerciseRepo.save() sets the generated id in the object
		exerciseRepo.save(exercise);
		int newId = exercise.getId();

		// send the generated id back to the client
		return Collections.singletonMap("id", newId);
	}
}
