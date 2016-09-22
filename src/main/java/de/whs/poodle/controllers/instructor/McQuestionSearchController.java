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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.whs.poodle.Utils;
import de.whs.poodle.beans.AbstractExercise.ExerciseType;
import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.Tag;
import de.whs.poodle.beans.mc.InstructorMcWorksheet;
import de.whs.poodle.beans.mc.McQuestion;
import de.whs.poodle.beans.mc.McQuestionSearchCriteria;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.InstructorMcWorksheetRepository;
import de.whs.poodle.repositories.InstructorRepository;
import de.whs.poodle.repositories.McQuestionRepository;
import de.whs.poodle.repositories.TagRepository;

@Controller
@RequestMapping("instructor/mcQuestionSearch")
public class McQuestionSearchController {

	@Autowired
	private McQuestionRepository mcQuestionRepo;

	@Autowired
	private TagRepository tagRepo;

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private InstructorRepository instructorRepo;

	@Autowired
	private InstructorMcWorksheetRepository instructorMcWorksheetRepo;

	@Autowired
	private Utils utils;

	public enum SearchMode {
		NORMAL, ADD_TO_MC_WORKSHEET;
	}

	@ModelAttribute
	public void populateModel(@ModelAttribute Instructor instructor, Model model) {
		List<Tag> tags = tagRepo.getForPublicCourses(instructor.getId(), ExerciseType.MC_QUESTION);
		List<Tag> distinctTags = tagRepo.getDistinctTags(tags);
		List<Course> courses = courseRepo.getAllForInstructor(instructor.getId());
		List<Instructor> instructors = instructorRepo.getExerciseCreatorsForPublicCourses(instructor.getId(), ExerciseType.MC_QUESTION);

		model.addAttribute("tags", tags);
		model.addAttribute("distinctTags", distinctTags);
		model.addAttribute("courses", courses);
		model.addAttribute("instructors", instructors);
	}

	@RequestMapping(method = RequestMethod.GET)
	public String get(Model model) {
		model.addAttribute("searchCriteria", new McQuestionSearchCriteria());
		return "instructor/mcQuestionSearch";
	}

	@RequestMapping(method = RequestMethod.GET, params="search=1")
	public String search(
			@ModelAttribute McQuestionSearchCriteria searchCriteria,
			@ModelAttribute Instructor instructor,
			@RequestParam(required=false) Integer instructorMcWorksheetId,
			Model model)
			throws SQLException {

		SearchMode searchMode;
		if (instructorMcWorksheetId != null) {
			InstructorMcWorksheet worksheet = instructorMcWorksheetRepo.getById(instructorMcWorksheetId);
			model.addAttribute("worksheet", worksheet);
			searchMode = SearchMode.ADD_TO_MC_WORKSHEET;
			searchCriteria.setFilterInstructorMcWorksheetId(instructorMcWorksheetId);
		}
		else {
			searchMode = SearchMode.NORMAL;
		}

		searchCriteria.setInstructorId(instructor.getId());

		if (searchCriteria.isEmpty()) {
			model.addAttribute("errorMessageCode", "noSearchCriteria");
		}
		else {
			List<McQuestion> questions = mcQuestionRepo.search(searchCriteria);
			model.addAttribute("questions", questions);
		}

		model.addAttribute("searchMode", searchMode);
		model.addAttribute("searchCriteria", searchCriteria);

		return "instructor/mcQuestionSearch";
	}


	@RequestMapping(value="addQuestionToWorksheet", method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> addQuestionToMcWorksheet(@RequestParam int mcWorksheetId, @RequestParam int mcQuestionId) {
		instructorMcWorksheetRepo.addQuestion(mcWorksheetId, mcQuestionId);
		return utils.simpleMessage("questionAdded");
	}
}
