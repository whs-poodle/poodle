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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.Exercise;
import de.whs.poodle.beans.statistics.Statistic;
import de.whs.poodle.beans.statistics.StatisticList;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.ExerciseRepository;
import de.whs.poodle.repositories.StatisticsRepository;
import de.whs.poodle.repositories.exceptions.ForbiddenException;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Controller
@RequestMapping("instructor/exercises/{exerciseId}")
public class InstructorExerciseController {

	@Autowired
	private ExerciseRepository exerciseRepo;

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private StatisticsRepository statisticsRepo;

	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("@instructorSecurity.hasAccessToExercise(authentication.name, #exerciseId)")
	public String get(
			@PathVariable int exerciseId,
			@RequestParam(required = false) boolean saveSuccess,
			@RequestParam(required = false) boolean oldRevision,
			Model model) {
		Exercise exercise = exerciseRepo.getById(exerciseId);
		if (exercise == null)
			throw new NotFoundException();

		Course course = courseRepo.getById(exercise.getCourseId());

		model.addAttribute("exercise", exercise);
		model.addAttribute("course", course);

		if (saveSuccess)
			model.addAttribute("okMessageCode", "exerciseSaved");

		List<Exercise> revisions = exerciseRepo.getAllRevisionsForRoot(exercise.getRootId());
		StatisticList statistics = statisticsRepo.getForExerciseRoot(exercise.getRootId());

		model.addAttribute("revisions", revisions);
		model.addAttribute("statistics", statistics);

		return "instructor/exercise";
	}

	@RequestMapping(method = RequestMethod.POST, params="delete")
	@PreAuthorize("@instructorSecurity.hasAccessToExercise(authentication.name, #exerciseId)")
	public String delete(Model model, @PathVariable int exerciseId, RedirectAttributes redirectAttributes) {
		try {
			exerciseRepo.delete(exerciseId);
		} catch(ForbiddenException e) {
			redirectAttributes.addFlashAttribute("errorMessageCode", "cantDeleteExercise");
			return "redirect:/instructor/exercises/{exerciseId}";
		}

		redirectAttributes.addFlashAttribute("okMessageCode", "exerciseDeleted");
		return "redirect:/instructor";
	}

	@RequestMapping(method = RequestMethod.GET, params={"oldId", "newId"})
	public String showDiff(@RequestParam int oldId, @RequestParam int newId) {
		return "redirect:/instructor/exerciseDiff/" + oldId + "/" + newId;
	}


	/*
	 * Returns the statistics for this exercise as a two dimensional array.
	 * We don't return the statistics itself here since the table can be
	 * used directly by the Google Charts API which simplifies things
	 * on the client.
	 */
	@RequestMapping(value = "chartData", method = RequestMethod.GET)
	@ResponseBody
	public Object[][] getChartData(@PathVariable int exerciseId) {
		Exercise exercise = exerciseRepo.getById(exerciseId);
		if (exercise == null)
			throw new NotFoundException();

		StatisticList statisticList = statisticsRepo.getForExerciseRoot(exercise.getRootId());
		List<Statistic> statistics = statisticList.getListWithoutEmptyOrIgnored();

		Object[][] table = new Object[statistics.size()][4];

		for (int i = 0; i < statistics.size(); i++) {
			Statistic s = statistics.get(i);
			table[i][0] = s.getStatus();
			table[i][1] = s.getDifficulty();
			table[i][2] = s.getFun();
			table[i][3] = s.getTime();
		}

		return table;
	}
}
