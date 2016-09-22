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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.whs.poodle.beans.AbstractExercise.ExerciseType;
import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.ExerciseSearchCriteria;
import de.whs.poodle.beans.ExerciseSearchResult;
import de.whs.poodle.beans.SelfStudyWorksheet;
import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.StudentToCourseTerm;
import de.whs.poodle.beans.Tag;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.ExerciseRepository;
import de.whs.poodle.repositories.StudentToCourseTermRepository;
import de.whs.poodle.repositories.TagRepository;
import de.whs.poodle.repositories.exceptions.BadRequestException;

@Controller
@RequestMapping("/student/exerciseSearch")
public class StudentExerciseSearchController {

	@Autowired
	private ExerciseRepository exerciseRepo;

	@Autowired
	private TagRepository tagRepo;

	@Autowired
	private StudentToCourseTermRepository studentToCourseTermRepo;

	@Autowired
	private CourseRepository courseRepo;

	@RequestMapping(method = RequestMethod.GET, params={"courseTermId", "search=1"})
	@PreAuthorize("@studentSecurity.hasAccessToCourseTerm(authentication.name, #courseTermId)")
	public String searchAndAddToWorksheet(
			@ModelAttribute ExerciseSearchCriteria searchCriteria,
			@RequestParam int courseTermId,
			@RequestParam(defaultValue = "0") int[] courses,
			@ModelAttribute Student student,
			Model model,
			HttpServletRequest request,
			HttpSession session)
			throws SQLException {

		int studentId = student.getId();
		StudentToCourseTerm studentToCourseTerm = studentToCourseTermRepo.get(studentId, courseTermId);
		CourseTerm courseTerm = studentToCourseTerm.getCourseTerm();
		SelfStudyWorksheet selfStudyWorksheet = studentToCourseTerm.getSelfStudyWorksheet();
		int courseId = courseTerm.getCourse().getId();
		List<Course> searchableCourses = courseRepo.getLinkedCurses(courseId);
		searchableCourses.add(courseTerm.getCourse());

		// worksheet is full, redirect to self study (the controller automatically shows a "worksheet is full" message).
		if (selfStudyWorksheet.isFull()) {
			return "redirect:/student/selfStudy/" + courseTerm.getId();
		}

		model.addAttribute("courseTerm", courseTerm);

		// this makes sure that exercises that are already on the worksheet are filtered from the results
		searchCriteria.getStudentFilter().setId(studentId);
		searchCriteria.getStudentFilter().setCourseTermId(courseTermId);

		// only tags that are used in this course
		List<Tag> tags = tagRepo.getForStudentInCourse(courseId, ExerciseType.EXERCISE);
		List<Tag> distinctTags = tagRepo.getDistinctTags(tags);

		searchCriteria.setCourses(courses);

		model.addAttribute("tags", tags);
		model.addAttribute("distinctTags", distinctTags);

		List<ExerciseSearchResult> exercises = exerciseRepo.search(searchCriteria);
		model.addAttribute("exercises", exercises);

		model.addAttribute("searchCriteria", searchCriteria);

		model.addAttribute("difficultyModes", ExerciseSearchCriteria.DifficultyMode.values());

		model.addAttribute("courses", searchableCourses);

		return "student/exerciseSearch";
	}


	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> addExerciseToSelfStudyWorksheet(
			@ModelAttribute Student student,
			@RequestParam int courseTermId,
			@RequestParam int exerciseId) {

		StudentToCourseTerm studentToCourseTerm = studentToCourseTermRepo.get(student.getId(), courseTermId);
		SelfStudyWorksheet selfStudyWorksheet = studentToCourseTerm.getSelfStudyWorksheet();
		if (selfStudyWorksheet.isFull())
			throw new BadRequestException("worksheetIsAlreadyFull");

		studentToCourseTermRepo.addExercise(student.getId(), courseTermId, exerciseId);

		int exercisesLeft = selfStudyWorksheet.getExercisesLeft() - 1;

		Map<String,Object> response = new HashMap<>();
		response.put("left", exercisesLeft);

		return response;
	}
}
