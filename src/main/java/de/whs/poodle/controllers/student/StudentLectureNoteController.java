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
package de.whs.poodle.controllers.student;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.LectureNote;
import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.StudentToCourseTerm;
import de.whs.poodle.repositories.LectureNoteRepository;
import de.whs.poodle.repositories.StudentToCourseTermRepository;

@Controller
@RequestMapping("/student/lectureNote/{courseTermId}")
public class StudentLectureNoteController {

	@Autowired
	private LectureNoteRepository lectureRepo;

	@Autowired
	private StudentToCourseTermRepository studentToCourseTermRepo;

	@RequestMapping(method = RequestMethod.GET)//, params={"courseTermId"})
	@PreAuthorize("@studentSecurity.hasAccessToCourseTerm(authentication.name, #courseTermId)")
	public String get(
			@ModelAttribute("isStudent") boolean isStudent,
			@ModelAttribute Student student,
			@PathVariable int courseTermId,
			Model model) {

		Integer id = lectureRepo.getCourseId(courseTermId);
		List<LectureNote> lectureNotes = lectureRepo.getForCourse(id);
		List<String> lectureGroups = lectureRepo.getGroupnames(id);
		StudentToCourseTerm studentToCourseTerm = studentToCourseTermRepo.get(student.getId(), courseTermId);
		CourseTerm courseTerm = studentToCourseTerm.getCourseTerm();

		model.addAttribute("lectureGroups", lectureGroups);
		model.addAttribute("lectureNotes", lectureNotes);
		model.addAttribute("courseTerm", courseTerm);

		return "student/lectureNote";
	}
}
