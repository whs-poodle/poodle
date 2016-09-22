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

import de.whs.poodle.beans.diff.McQuestionDiff;
import de.whs.poodle.beans.mc.McQuestion;
import de.whs.poodle.repositories.McQuestionRepository;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Controller
@RequestMapping("/instructor/mcQuestionDiff")
public class McQuestionDiffController {

	@Autowired
	private McQuestionRepository mcQuestionRepo;

	@RequestMapping("/{questionId1}/{questionId2}")
	public String showDiff(@PathVariable int questionId1, @PathVariable int questionId2, Model model) {
		McQuestion question1 = mcQuestionRepo.getById(questionId1);
		if (question1 == null)
			throw new NotFoundException();

		McQuestion question2 = mcQuestionRepo.getById(questionId2);
		if (question2 == null)
			throw new NotFoundException();

		McQuestionDiff questionDiff = new McQuestionDiff(question1, question2);
		model.addAttribute("question1", question1);
		model.addAttribute("question2", question2);
		model.addAttribute("questionDiff", questionDiff);

		return "instructor/mcQuestionDiff";
	}
}
