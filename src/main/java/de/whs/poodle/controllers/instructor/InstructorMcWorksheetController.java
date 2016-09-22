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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import de.whs.poodle.beans.mc.InstructorMcWorksheet;
import de.whs.poodle.beans.mc.McQuestion.Answer;
import de.whs.poodle.beans.mc.McQuestionOnWorksheet;
import de.whs.poodle.beans.mc.McStatistic;
import de.whs.poodle.beans.mc.McWorksheet;
import de.whs.poodle.repositories.InstructorMcWorksheetRepository;
import de.whs.poodle.repositories.McStatisticsRepository;
import de.whs.poodle.repositories.McWorksheetRepository;

@Controller
@RequestMapping("instructor/mcWorksheets/")
public class InstructorMcWorksheetController {

	@Autowired
	private InstructorMcWorksheetRepository instructorMcWorksheetRepo;

	@Autowired
	private McWorksheetRepository mcWorksheetRepo;

	@Autowired
	private McStatisticsRepository mcStatisticsRepo;

	@RequestMapping(value="{worksheetId}")
	@PreAuthorize("@instructorSecurity.hasAccessToWorksheet(authentication.name, #worksheetId)")
	public String get(@PathVariable int worksheetId, Model model) {
		InstructorMcWorksheet worksheet = instructorMcWorksheetRepo.getById(worksheetId);
		int mcWorksheetId = worksheet.getMcWorksheetId();
		return "redirect:show/" + mcWorksheetId;
	}

	@RequestMapping(value="show/{mcWorksheetId}")
	@PreAuthorize("@instructorSecurity.hasAccessToMcWorksheet(authentication.name, #mcWorksheetId)")
	public String show(@PathVariable int mcWorksheetId, Model model) {
		McWorksheet worksheet = mcWorksheetRepo.getByMcWorksheetId(mcWorksheetId);

		boolean isStudentWorksheet = true;
		boolean isUnlockedWorksheet = true;

		if (worksheet.getMcWorksheetType() == McWorksheet.McWorksheetType.INSTRUCTOR)
		{
			InstructorMcWorksheet ws = instructorMcWorksheetRepo.getByMcWorksheetId(mcWorksheetId);

			isUnlockedWorksheet = ws.isUnlocked();
			isStudentWorksheet = false;
		}

		model.addAttribute("isUnlockedWorksheet", isUnlockedWorksheet);
		model.addAttribute("isStudentWorksheet", isStudentWorksheet);
		model.addAttribute("worksheet", worksheet);
		return "instructor/mcWorksheet";
	}

	/*
	 * Returns the answer stats for a MC question on a worksheet, i.e. how many
	 * times each of the answers has been chosen. This is used in instructor/mcWorksheet.js
	 * to generate the charts for each of the questions.
	 *
	 * Each entry in the returned list contains the Answer object and the number of
	 * times it has been chosen (count).
	 */
	@RequestMapping("show/answerStats/{mcWorksheetToQuestionId}")
	@ResponseBody
	public List<Map<String,Object>> getAnswerStatsForQuestionOnWorksheet(@PathVariable int mcWorksheetToQuestionId) {
		McQuestionOnWorksheet questionOnWorksheet = mcWorksheetRepo.getQuestionOnWorksheetById(mcWorksheetToQuestionId);

		List<McStatistic> statistics = mcStatisticsRepo.getForQuestionOnWorksheet(mcWorksheetToQuestionId);

		List<Map<String,Object>> answerWithCountList = new ArrayList<>();

		// iterate the answers of this question...
		for (Answer answer : questionOnWorksheet.getQuestion().getAnswers()) {
			// ..and count how many times it has been chosen in the statistics
			int count = (int)statistics.stream()
					.filter(s -> s.getAnswers().contains(answer))
					.count();

			Map<String,Object> answerWithCount = new HashMap<>();
			answerWithCount.put("answer", answer);
			answerWithCount.put("count", count);

			answerWithCountList.add(answerWithCount);
		}

		return answerWithCountList;
	}
}
