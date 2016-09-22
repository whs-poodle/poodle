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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.evaluation.EvaluationWorksheet;
import de.whs.poodle.beans.evaluation.StudentEvaluationData;
import de.whs.poodle.repositories.EvaluationWorksheetRepository;
import de.whs.poodle.repositories.exceptions.NotFoundException;

/*
 * Controller for a student to fill out an evaluation.
 */
@Controller
@RequestMapping("/student/evaluation/{courseTermId}")
public class StudentEvaluationController {

	@Autowired
	private EvaluationWorksheetRepository evaluationWorksheetRepo;

	@RequestMapping
	@PreAuthorize("@studentSecurity.hasAccessToEvaluation(authentication.name, #courseTermId)")
	public String get(@PathVariable int courseTermId, @ModelAttribute Student student, Model model) {
		EvaluationWorksheet worksheet = evaluationWorksheetRepo.getForCourseTerm(courseTermId);
		if (worksheet == null)
			throw new NotFoundException();

		model.addAttribute("worksheet", worksheet);
		return "student/evaluation";
	}

	// used via JS to save the evaluation
	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("@studentSecurity.hasAccessToEvaluation(authentication.name, #courseTermId)")
	@ResponseBody
	public void saveEvaluation(@PathVariable int courseTermId, @ModelAttribute Student student, @RequestBody StudentEvaluationData data) {
		EvaluationWorksheet worksheet = evaluationWorksheetRepo.getForCourseTerm(courseTermId);

		evaluationWorksheetRepo.saveStudentEvaluation(worksheet.getId(), student.getId(), data);
	}
}
