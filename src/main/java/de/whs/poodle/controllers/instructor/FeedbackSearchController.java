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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.forms.FeedbackSearchCriteria;
import de.whs.poodle.beans.statistics.Statistic;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.StatisticsRepository;

@Controller
@RequestMapping("instructor/feedback")
public class FeedbackSearchController {

	private static final int FEEDBACK_LIMIT = 200;

	@Autowired
	private StatisticsRepository statisticsRepo;

	@Autowired
	private CourseRepository courseRepo;

	@RequestMapping(method = RequestMethod.GET)
	public String get(
			@ModelAttribute Instructor instructor,
			@ModelAttribute FeedbackSearchCriteria searchCriteria,
			Model model) {

		List<Statistic> feedbackList = statisticsRepo.getStatistics(searchCriteria, instructor.getId(), FEEDBACK_LIMIT);
		List<Course> courses = courseRepo.getAllForInstructor(instructor.getId());

		model.addAttribute("feedbackList", feedbackList);
		model.addAttribute("courses", courses);
		model.addAttribute("searchCriteria", searchCriteria);

		return "instructor/feedback";
	}
}
