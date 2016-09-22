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

import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.whs.poodle.beans.AbstractExercise.ExerciseType;
import de.whs.poodle.beans.Chapter;
import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.ExerciseSearchCriteria;
import de.whs.poodle.beans.ExerciseSearchResult;
import de.whs.poodle.beans.ExerciseWorksheet;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.Tag;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.ExerciseRepository;
import de.whs.poodle.repositories.ExerciseWorksheetRepository;
import de.whs.poodle.repositories.InstructorRepository;
import de.whs.poodle.repositories.TagRepository;

@Controller
@RequestMapping("/instructor/exerciseSearch")
public class InstructorExerciseSearchController {

	@Autowired
	private ExerciseRepository exerciseRepo;

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private InstructorRepository instructorRepo;

	@Autowired
	private TagRepository tagRepo;

	@Autowired
	private ExerciseWorksheetRepository exerciseWorksheetRepo;

	public enum SearchMode {
		NORMAL, ADD_TO_CHAPTER, LINK_EXERCISE;
	}

	@ModelAttribute
	public void populateModel(@ModelAttribute Instructor instructor, Model model) {
		List<Course> courses = courseRepo.getAllForInstructor(instructor.getId());
		List<Tag> tags = tagRepo.getForPublicCourses(instructor.getId(), ExerciseType.EXERCISE);
		List<Tag> distinctTags = tagRepo.getDistinctTags(tags);
		List<Instructor> instructors = instructorRepo.getExerciseCreatorsForPublicCourses(instructor.getId(), ExerciseType.EXERCISE);

		model.addAttribute("courses", courses);
		model.addAttribute("tags", tags);
		model.addAttribute("distinctTags", distinctTags);
		model.addAttribute("instructors", instructors);
		model.addAttribute("difficultyModes", ExerciseSearchCriteria.DifficultyMode.values());
	}

	@RequestMapping(method = RequestMethod.GET)
	public String get(Model model) {
		model.addAttribute("searchCriteria", new ExerciseSearchCriteria());
		model.addAttribute("searchMode", SearchMode.NORMAL);
		return "instructor/exerciseSearch";
	}

	@RequestMapping(method = RequestMethod.GET, params="search=1")
	public String search(
			@ModelAttribute Instructor instructor,
			@ModelAttribute ExerciseSearchCriteria searchCriteria,
			@RequestParam(defaultValue = "0") int worksheetId,
			@RequestParam(defaultValue = "0") int chapterId,
			@RequestParam(defaultValue = "0") boolean exerciseLink,
			Model model,
			HttpServletRequest request,
			HttpSession session)
			throws SQLException {

		SearchMode searchMode;
		searchCriteria.setInstructorId(instructor.getId());

		/* These are passed by the worksheet editor so wen switch to the ADD_TO_CHAPTER mode and
		 * show an "add button" etc. */
		if (worksheetId != 0 && chapterId != 0) {
			ExerciseWorksheet worksheet = exerciseWorksheetRepo.getById(worksheetId);

			// we need	the courseTerm ID to filter out exercises which already exist on one of the worksheets
			searchCriteria.setWorksheetFilter(worksheet.getCourseTerm().getId());

			// get the chapter so we can display it in the page title
			Chapter chapter = null;
			for(Chapter k : worksheet.getChapters()) {
				if (k.getId() == chapterId) {
					chapter = k;
					break;
				}
			}

			searchMode = SearchMode.ADD_TO_CHAPTER;
			model.addAttribute("worksheet", worksheet);
			model.addAttribute("chapter", chapter);
		}
		else if (exerciseLink) {
			searchMode = SearchMode.LINK_EXERCISE;
		}
		else {
			searchMode = SearchMode.NORMAL;
		}

		model.addAttribute("searchMode", searchMode);

		List<ExerciseSearchResult> exercises = exerciseRepo.search(searchCriteria);
		model.addAttribute("exercises", exercises);

		// save parameters in the session for the "last search" function
		String searchParamsStr = request.getQueryString();
		session.setAttribute("lastSearch", searchParamsStr);

		model.addAttribute("searchCriteria", searchCriteria);
		return "instructor/exerciseSearch";
	}


	@RequestMapping(value = "/addExerciseToChapter", method = RequestMethod.POST)
	@ResponseBody
	public void addExerciseToChapter(@RequestParam int chapterId, @RequestParam int exerciseId) {
		exerciseWorksheetRepo.addExerciseToChapter(chapterId, exerciseId);
	}
}
