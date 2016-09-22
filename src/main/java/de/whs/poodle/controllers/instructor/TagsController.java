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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.whs.poodle.Utils;
import de.whs.poodle.beans.AbstractExercise.ExerciseType;
import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.CourseTagManagement;
import de.whs.poodle.beans.Tag;
import de.whs.poodle.repositories.TagRepository;

@Controller
@RequestMapping("instructor/courses/{courseId}/tags")
public class TagsController {

	@Autowired
	private TagRepository tagRepo;

	@Autowired
	private Utils utils;

	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String get(
			@PathVariable int courseId,
			Model model,
			@ModelAttribute("globalCourses") List<Course> courses) {
		CourseTagManagement ctm = tagRepo.getCourseTagManagement(courseId, courses);
		model.addAttribute("ctm", ctm);
		return "instructor/tags";
	}

	@RequestMapping(value = "/deleteTag", method = RequestMethod.POST)
	@ResponseBody
	public void deleteTag(@RequestParam int tagId) {
		tagRepo.deleteTag(tagId);
	}


	@RequestMapping(value = "getTags", method = RequestMethod.GET)
	@ResponseBody
	public List<Tag> getTagsForCourse(@PathVariable int courseId) {
		return tagRepo.getForCourse(courseId, ExerciseType.ALL);
	}


	@RequestMapping(value = "mergeTags", method = RequestMethod.POST)
	@ResponseBody
	public void mergeTags(
			@RequestParam List<Integer> tagIds,
			@RequestParam int mergeTo) {
		tagRepo.mergeTags(tagIds, mergeTo);
	}

	@RequestMapping(value="renameTag", method = RequestMethod.POST)
	@ResponseBody
	public void renameTag(
			@RequestParam int tagId,
			@RequestParam String name) {
		tagRepo.renameTag(tagId, name);
	}

	@RequestMapping(value="addTagToCourse", method = RequestMethod.POST)
	@ResponseBody
	public void addTagToCourse(@PathVariable int courseId, @RequestParam int tagId){
		tagRepo.addTagToCourse(courseId, tagId);
	}

	@RequestMapping(value="changeInstructorOnly", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> changeInstructorOnly(@RequestParam int tagId, @RequestParam boolean instructorOnly){
		tagRepo.changeInstructorOnly(tagId, instructorOnly);

		return utils.simpleMessage("instructorOnlySet." + instructorOnly);
	}


}
