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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import de.whs.poodle.beans.ExerciseWorksheet;
import de.whs.poodle.beans.statistics.WorksheetStatistics;
import de.whs.poodle.repositories.ExerciseWorksheetRepository;
import de.whs.poodle.repositories.StatisticsRepository;

@Controller
@RequestMapping("instructor/worksheets/{worksheetId}")
public class InstructorWorksheetController {

	@Autowired
	private ExerciseWorksheetRepository exerciseWorksheetRepo;

	@Autowired
	private StatisticsRepository statisticsRepo;

	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String get(@PathVariable int worksheetId, Model model) {
		ExerciseWorksheet worksheet = exerciseWorksheetRepo.getById(worksheetId);
		WorksheetStatistics worksheetStatistics = statisticsRepo.getWorksheetStatistics(worksheet);

		model.addAttribute("worksheet", worksheet);
		model.addAttribute("worksheetStatistics", worksheetStatistics);

		return "instructor/worksheet";
	}
}
