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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import de.whs.poodle.beans.AbstractExercise;
import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.Exercise;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.forms.FeedbackSearchCriteria;
import de.whs.poodle.beans.mc.McQuestion;
import de.whs.poodle.beans.statistics.Statistic;
import de.whs.poodle.repositories.ExerciseRepository;
import de.whs.poodle.repositories.McQuestionRepository;
import de.whs.poodle.repositories.StatisticsRepository;

@Controller
@RequestMapping("/instructor")
public class InstructorStartController {

	private static final int FEEDBACK_MAX = 10;
	private static final int CHANGES_MAX = 50;

	@Autowired
	private ExerciseRepository exerciseRepo;

	@Autowired
	private McQuestionRepository mcQuestionRepo;

	@Autowired
	private StatisticsRepository statisticsRepo;

	@RequestMapping(method = RequestMethod.GET)
	public String get(@ModelAttribute Instructor instructor, Model model, @ModelAttribute("globalCourses") ArrayList<Course> courses) {
		if (courses.isEmpty())
			return "instructor/firstStart";

		List<AbstractExercise> exercises = getLatestChanges(instructor.getId());
		List<Statistic> feedbackList = statisticsRepo.getStatistics(new FeedbackSearchCriteria(), instructor.getId(), FEEDBACK_MAX);

		model.addAttribute("exercises", exercises);
		model.addAttribute("feedbackList", feedbackList);
		return "instructor/start";
	}

	private List<AbstractExercise> getLatestChanges(int instructorId) {
		/* load the latest changes for exercises and mcQuestions and
		 * then merge both into a List<AbstractExercise> .*/
		List<Exercise> exercises = exerciseRepo.getLatestExercises(instructorId, CHANGES_MAX);
		List<McQuestion> mcQuestions = mcQuestionRepo.getLatest(instructorId, CHANGES_MAX);

		// merge
		List<AbstractExercise> allExercises = new ArrayList<>();
		allExercises.addAll(exercises);
		allExercises.addAll(mcQuestions);

		// sort
		allExercises.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

		// trim list to CHANGES_MAX
		if (allExercises.size() > CHANGES_MAX)
			allExercises = allExercises.subList(0, CHANGES_MAX - 1);

		return allExercises;
	}
}