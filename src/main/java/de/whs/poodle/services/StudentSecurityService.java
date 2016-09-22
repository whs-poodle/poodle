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

import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.Worksheet;
import de.whs.poodle.beans.evaluation.EvaluationWorksheet;
import de.whs.poodle.beans.mc.InstructorMcWorksheet;
import de.whs.poodle.beans.mc.McWorksheet;
import de.whs.poodle.beans.mc.McWorksheet.McWorksheetType;
import de.whs.poodle.beans.mc.StudentMcWorksheet;
import de.whs.poodle.repositories.EvaluationWorksheetRepository;
import de.whs.poodle.repositories.InstructorMcWorksheetRepository;
import de.whs.poodle.repositories.McWorksheetRepository;
import de.whs.poodle.repositories.StudentRepository;
import de.whs.poodle.repositories.WorksheetRepository;

/*
 * Security related functions for students, see also InstructorSecurityService.
 */
@Service("studentSecurity")
public class StudentSecurityService {

	private static Logger log = LoggerFactory.getLogger(StudentSecurityService.class);

	@Autowired
	private StudentRepository studentRepo;

	@Autowired
	private WorksheetRepository worksheetRepo;

	@Autowired
	private EvaluationWorksheetRepository evaluationWorksheetRepo;

	@Autowired
	private McWorksheetRepository mcWorksheetRepo;

	@Autowired
	private InstructorMcWorksheetRepository instructorMcWorksheetRepo;

	@Autowired
	private JdbcTemplate jdbc;

	public boolean hasAccessToWorksheet(String username, int worksheetId) {
		log.debug("checking if student {} has access to worksheet {}", username, worksheetId);

		Worksheet worksheet = worksheetRepo.getById(worksheetId);
		if (worksheet == null)
			return false;

		return worksheet.isUnlocked() &&
			isEnrolled(username, worksheet.getCourseTerm().getId());
	}

	public boolean hasAccessToCourseTerm(String username, int courseTermId) {
		log.debug("checking if student {} has access to course term {}", username, courseTermId);
		return isEnrolled(username, courseTermId);
	}

	public boolean hasAccessToEvaluation(String username, int courseTermId) {
		log.debug("checking if student {} has access to evaluation of course term {}", username, courseTermId);
		EvaluationWorksheet evaluation = evaluationWorksheetRepo.getForCourseTerm(courseTermId);
		if (evaluation == null)
			return false;

		return evaluation.isAccessibleForStudents() &&
				isEnrolled(username, courseTermId);
	}

	public boolean hasAccessToMcWorksheet(String username, int mcWorksheetId) {
		log.debug("checking if student {} has access to mcWorksheet {}", username, mcWorksheetId);

		McWorksheet worksheet = mcWorksheetRepo.getByMcWorksheetId(mcWorksheetId);
		if (worksheet == null)
			return false;

		if (worksheet.getMcWorksheetType() == McWorksheetType.INSTRUCTOR) {
			InstructorMcWorksheet instructorMcWorksheet =
					instructorMcWorksheetRepo.getById(worksheet.getId());

			return instructorMcWorksheet.isUnlocked() &&
				isEnrolled(username, worksheet.getCourseTerm().getId());
		}
		else { // StudentMcWorksheet
			StudentMcWorksheet studentMcWorksheet =
					mcWorksheetRepo.getStudentMcWorksheetById(worksheet.getId());

			return studentMcWorksheet.isPublic() ||
					studentMcWorksheet.getStudent().getUsername().equals(username);
		}
	}

	private boolean isEnrolled(String username, int courseTermId) {
		Student student = studentRepo.getByUsername(username);
		if (student == null)
			return false;

		return jdbc.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM student_to_course_term WHERE student_id = ? AND course_term_id = ?)",
				new Object[]{student.getId(), courseTermId}, Boolean.class);
	}
}
