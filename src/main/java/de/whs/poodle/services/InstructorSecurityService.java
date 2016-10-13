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
package de.whs.poodle.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.Exercise;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.Worksheet;
import de.whs.poodle.beans.mc.McQuestion;
import de.whs.poodle.beans.mc.McWorksheet;
import de.whs.poodle.repositories.CourseTermRepository;
import de.whs.poodle.repositories.ExerciseRepository;
import de.whs.poodle.repositories.InstructorRepository;
import de.whs.poodle.repositories.McQuestionRepository;
import de.whs.poodle.repositories.McWorksheetRepository;
import de.whs.poodle.repositories.WorksheetRepository;

/*
 * We use the functions here for some security related checks in @PreAuthorize
 * annotations etc. For example, we use the hasAccessToCourse() function
 * in the @PreAuthorize annotation on ManageCourseController to make sure that an instructor
 * can't access courses by other instructors.
 *
 * Note that we pass the username in the functions here since we have no access to the ID of
 * the logged in user within the expression in the @PreAuthorize annotation.
 */
@Service("instructorSecurity")
public class InstructorSecurityService {

	private static Logger log = LoggerFactory.getLogger(InstructorSecurityService.class);

	@Autowired
	private InstructorRepository instructorRepo;

	@Autowired
	private WorksheetRepository worksheetRepo;

	@Autowired
	private ExerciseRepository exerciseRepo;

	@Autowired
	private McQuestionRepository mcQuestionRepo;

	@Autowired
	private McWorksheetRepository mcWorksheetRepo;

	@Autowired
	private CourseTermRepository courseTermRepo;

	@Autowired
	private JdbcTemplate jdbc;

	public boolean hasAccessToCourse(String username, int courseId) {
		log.debug("checking if instructor {} has access to course {}", username, courseId);
		return hasAccessToCourseById(username, courseId);
	}

	public boolean hasAccessToCourseTerm(String username, int courseTermId) {
		log.debug("checking if instructor {} has access to course term {}", username, courseTermId);

		CourseTerm courseTerm = courseTermRepo.getById(courseTermId);
		if (courseTerm == null)
			return false;

		int courseId = courseTerm.getCourse().getId();

		return hasAccessToCourseById(username, courseId);
	}

	public boolean hasAccessToWorksheet(String username, int worksheetId) {
		Worksheet worksheet = worksheetRepo.getById(worksheetId);
		if (worksheet == null)
			return false;

		int courseId = worksheet.getCourseTerm().getCourse().getId();
		return hasAccessToCourseById(username, courseId);
	}

	public boolean hasAccessToMcWorksheet(String username, int mcWorksheetId) {
		log.debug("checking if instructor {} has access to mcWorksheet {}", username, mcWorksheetId);
		McWorksheet mcWorksheet = mcWorksheetRepo.getByMcWorksheetId(mcWorksheetId);
		if (mcWorksheet == null)
			return false;

		if (mcWorksheet.getMcWorksheetType() == McWorksheet.McWorksheetType.STUDENT)
			if(!mcWorksheetRepo.getStudentMcWorksheetById(mcWorksheetId).isPublic())
				return false;

		int courseId = mcWorksheet.getCourseTerm().getCourse().getId();
		return hasAccessToCourseById(username, courseId);
	}

	public boolean hasAccessToExercise(String username, int exerciseId) {
		log.debug("checking if instructor {} has access to exercise {}", username, exerciseId);
		Exercise exercise = exerciseRepo.getById(exerciseId);
		if (exercise == null)
			return false;

		return hasAccessToCourseById(username, exercise.getCourseId());
	}

	public boolean hasAccessToMcQuestion(String username, int mcQuestionId) {
		log.debug("checking if instructor {} has access to mcQuestion {}", username, mcQuestionId);

		McQuestion question = mcQuestionRepo.getById(mcQuestionId);
		if (question == null)
			return false;

		return hasAccessToCourseById(username, question.getCourseId());
	}

	private boolean hasAccessToCourseById(String username, int courseId) {
		Instructor instructor = instructorRepo.getByUsername(username);
		if (instructor == null)
			return false;

		return jdbc.queryForObject(
				"SELECT has_instructor_access_to_course(?,?)",
				new Object[]{courseId, instructor.getId()},
				Boolean.class);
	}

	public boolean hasAdminAccess(String username) {
		log.debug("checking if instructor {} is Admin", username);

		Instructor instructor = instructorRepo.getByUsername(username);
		if (instructor == null)
			return false;

		return instructor.isAdmin();
	}
}
