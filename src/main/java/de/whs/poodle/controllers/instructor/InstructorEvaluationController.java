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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import de.whs.poodle.beans.evaluation.EvaluationQuestion;
import de.whs.poodle.beans.evaluation.EvaluationQuestionStats;
import de.whs.poodle.beans.evaluation.EvaluationStatistic;
import de.whs.poodle.beans.evaluation.EvaluationWorksheet;
import de.whs.poodle.repositories.EvaluationWorksheetRepository;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Controller
@RequestMapping("/instructor/evaluation/{courseTermId}")
public class InstructorEvaluationController {

	@Autowired
	private EvaluationWorksheetRepository evaluationWorksheetRepo;

	@RequestMapping
	@PreAuthorize("@instructorSecurity.hasAccessToCourseTerm(authentication.name, #courseTermId)")
	public String get(@PathVariable int courseTermId, Model model) {
		EvaluationWorksheet worksheet = evaluationWorksheetRepo.getForCourseTerm(courseTermId);
		if (worksheet == null)
			throw new NotFoundException();


		Map<EvaluationQuestion,List<String>> questionToTextsMap =
				evaluationWorksheetRepo.getQuestionToTextsMapForEvaluation(worksheet.getId());

		model.addAttribute("worksheet", worksheet);
		model.addAttribute("questionToTextsMap", questionToTextsMap);
		return "instructor/evaluation";
	}

	@RequestMapping("choiceStats/{evaluationQuestionId}")
	@ResponseBody
	@PreAuthorize("@instructorSecurity.hasAccessToCourseTerm(authentication.name, #courseTermId)")
	public EvaluationQuestionStats getQuestionStats(@PathVariable int courseTermId, @PathVariable int evaluationQuestionId) {
		EvaluationQuestion question = evaluationWorksheetRepo.getQuestionById(evaluationQuestionId);
		List<EvaluationStatistic> stats = evaluationWorksheetRepo.getStatisticsForQuestion(evaluationQuestionId);

		return new EvaluationQuestionStats(question, stats);
	}
}
