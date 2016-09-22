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
package de.whs.poodle.controllers.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.whs.poodle.beans.mc.McWorksheet;
import de.whs.poodle.beans.mc.McWorksheetResults;
import de.whs.poodle.repositories.McStatisticsRepository;
import de.whs.poodle.repositories.McWorksheetRepository;

/*
 * REST functions used by instructors and students.
 */
@RestController
@RequestMapping("/common/rest/")
public class CommonRestController {

	@Autowired
	private McWorksheetRepository mcWorksheetRepo;

	@Autowired
	private McStatisticsRepository mcStatisticsRepo;

	/*
	 * Returns the highscores for a MC worksheet. This is used in
	 * js/mcWorksheetHighscoreChart.js to generate the chart.
	 *
	 * We return a map containt the maximum number of points on the worksheets
	 * and the highscore list. Each highscore list entry contains the student
	 * and the points that he got for the worksheet.
	 */
	@RequestMapping("mcWorksheetHighscore/{mcWorksheetId}")
	public Map<String,Object> getHighscoreForMcWorksheet(@PathVariable int mcWorksheetId) {
		List<Map<String,Object>> highscoreList = new ArrayList<>();

		McWorksheet worksheet = mcWorksheetRepo.getByMcWorksheetId(mcWorksheetId);
		List<McWorksheetResults> resultsList = mcStatisticsRepo.getHighscoresForWorksheet(worksheet);

		for (McWorksheetResults results : resultsList) {
			Map<String,Object> entry = new HashMap<>();
			entry.put("student", results.getStudent().getIdString());
			entry.put("points", results.getPoints());

			highscoreList.add(entry);
		}

		Map<String,Object> data = new HashMap<>();
		data.put("maxWorksheetPoints", worksheet.getMaxPoints());
		data.put("highscoreList", highscoreList);

		return data;
	}
}
