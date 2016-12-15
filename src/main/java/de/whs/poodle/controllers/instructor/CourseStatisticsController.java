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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.statistics.CourseStatistics;
import de.whs.poodle.beans.statistics.TotalCourseTermStatistics;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.CourseStatisticsRepository;
import de.whs.poodle.repositories.CourseTermRepository;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Controller
@RequestMapping("/instructor/courses/{courseId}/statistics")
public class CourseStatisticsController {

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private CourseTermRepository courseTermRepo;

	@Autowired
	private CourseStatisticsRepository courseStatisticsRepo;

	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String get(@PathVariable int courseId, Model model) {
		Course course = courseRepo.getById(courseId);
		if (course == null)
			throw new NotFoundException();

		CourseStatistics courseStatistics = courseStatisticsRepo.getForCourse(courseId);
		courseStatisticsRepo.createTotalCourseStatistic(courseStatistics);
		List<CourseTerm> courseTerms = courseTermRepo.getForCourse(courseId);

		model.addAttribute("course", course);
		model.addAttribute("courseStatistics", courseStatistics);
		model.addAttribute("courseTerms", courseTerms);

		return "instructor/courseStatistics";
	}


	@RequestMapping("dailyStatistics")
	@ResponseBody
	public Map<LocalDate, TotalCourseTermStatistics> getDailyStatistics(@RequestParam int courseTermId) {
		return courseStatisticsRepo.getDailyStatisticsForCourseTerm(courseTermId);
	}

	@RequestMapping("courseStatistics")
	@ResponseBody
	public Map<LocalDate, TotalCourseTermStatistics> getCourseStatistics(@RequestParam int courseId) {
		return courseStatisticsRepo.getStatisticsForCourse(courseId);
	}
}
