/*
 * Copyright 2016 Westfälische Hochschule
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.LectureNote;
import de.whs.poodle.beans.PoodleFile;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.LectureNoteRepository;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Controller
@RequestMapping("/instructor/courses/{courseId}/lectureNote")
public class InstructorLectureNoteController {

	@Autowired
	private CourseRepository courseRepo;

	@Autowired
	private LectureNoteRepository lectureNoteRepo;

	@Autowired
	private JdbcTemplate jdbc;

	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String get(@PathVariable int courseId, Model model) {
		Course course = courseRepo.getById(courseId);
		if (course == null)
			throw new NotFoundException();

		List<LectureNote> lectureNotes = lectureNoteRepo.getForCourse(courseId);
		List<String> lectureGroups = lectureNoteRepo.getGroupnames(courseId);

		model.addAttribute("lectureGroups", lectureGroups);
		model.addAttribute("lectureNotes", lectureNotes);
		model.addAttribute("course", course);

		return "instructor/lectureNote";
	}

	@RequestMapping(method = RequestMethod.POST, params = "newLectureNote")
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String addNote(
			@ModelAttribute LectureNote lectureNote,
			@PathVariable int courseId,
			@RequestParam String newLectureNote,
			@RequestParam int lectureNoteFileId,
			@RequestParam String lectureGroup,
			RedirectAttributes redirectAttributes) {

		int count = lectureNoteRepo.getNodeCountForGroup(lectureGroup) + 1;

		jdbc.queryForObject(
				"INSERT INTO lecture_note(title,groupname,num,course_id,file_id) VALUES(?,?,?,?,?) RETURNING id",
				new Object[]{newLectureNote, lectureGroup, count, courseId, lectureNoteFileId},
				Integer.class);

			lectureNote.setFile(new PoodleFile(lectureNoteFileId));

			return "redirect:/instructor/courses/{courseId}/lectureNote";
	}

	@RequestMapping(method = RequestMethod.POST, params="deleteLectureNoteId")
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String deleteNote(@PathVariable int courseId, @RequestParam int deleteLectureNoteId) {
		lectureNoteRepo.delete(courseId, deleteLectureNoteId);
		return "redirect:/instructor/courses/{courseId}/lectureNote";
	}

	@RequestMapping(method = RequestMethod.POST, params="renameNote")
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String renameNote(@PathVariable int courseId, @RequestParam int lectureNoteFileId, @RequestParam String newNoteTitle) {
		lectureNoteRepo.rename(lectureNoteFileId, newNoteTitle);
		return "redirect:/instructor/courses/{courseId}/lectureNote";
	}

	@RequestMapping(method = RequestMethod.POST, params="editFile")
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String editFile(@PathVariable int courseId, @RequestParam int oldLectureNoteFileId, @RequestParam int newLectureNoteFileId) {
		lectureNoteRepo.editFile(courseId, oldLectureNoteFileId, newLectureNoteFileId);
		return "redirect:/instructor/courses/{courseId}/lectureNote";
	}

	@RequestMapping(method = RequestMethod.POST, params={"noteId", "moveUp"})
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String move(@PathVariable int courseId, @RequestParam int noteId, @RequestParam boolean moveUp) {
		lectureNoteRepo.move(noteId, moveUp);
		return "redirect:/instructor/courses/{courseId}/lectureNote";
	}
}