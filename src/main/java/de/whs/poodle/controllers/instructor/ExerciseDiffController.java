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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import de.whs.poodle.beans.Exercise;
import de.whs.poodle.beans.diff.ExerciseDiff;
import de.whs.poodle.repositories.ExerciseRepository;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Controller
@RequestMapping("/instructor/exerciseDiff")
public class ExerciseDiffController {

	@Autowired
	private ExerciseRepository exerciseRepo;

	@RequestMapping("/{exerciseId1}/{exerciseId2}")
	public String showDiff(@PathVariable int exerciseId1, @PathVariable int exerciseId2, Model model) {
		Exercise exercise1 = exerciseRepo.getById(exerciseId1);
		if (exercise1 == null)
			throw new NotFoundException();

		Exercise exercise2 = exerciseRepo.getById(exerciseId2);
		if (exercise2 == null)
			throw new NotFoundException();

		ExerciseDiff exerciseDiff = new ExerciseDiff(exercise1, exercise2);
		model.addAttribute("exercise1", exercise1);
		model.addAttribute("exercise2", exercise2);
		model.addAttribute("exerciseDiff", exerciseDiff);

		return "instructor/exerciseDiff";
	}
}
