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
package de.whs.poodle.repositories;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import de.whs.poodle.beans.mc.InstructorMcWorksheet;
import de.whs.poodle.beans.mc.McStatistic;
import de.whs.poodle.beans.mc.McWorksheet;
import de.whs.poodle.beans.mc.McWorksheetResults;
import de.whs.poodle.beans.mc.StudentMcWorksheet;

@Repository
public class McStatisticsRepository {


	/*
	 * Maximum number of public worksheets by other students
	 * displayed on the MC page.
	 */
	private static final int LATEST_PUBLIC_WORKSHEETS_LIMIT = 10;

	/*
	 * Maximum number of entries in the "own results" table
	 * on the MC page.
	 */
	private static final int OWN_RESULTS_LIMIT = 10;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;

	public List<McWorksheetResults> getOwnStudentMcWorksheetsResults(int studentId, int courseTermId) {
		List<StudentMcWorksheet> worksheets = em.createQuery(
				"FROM StudentMcWorksheet " +
				"WHERE courseTerm.id = :courseTermId " +
				"AND FUNCTION('has_student_completed_mc_worksheet', :studentId, id) = TRUE " +
				"ORDER BY createdAt DESC",
				StudentMcWorksheet.class)
				.setParameter("studentId", studentId)
				.setParameter("courseTermId", courseTermId)
				.setMaxResults(OWN_RESULTS_LIMIT)
				.getResultList();

		return worksheets.stream()
				.map(worksheet -> getStudentMcWorksheetResults(worksheet, studentId))
				.collect(Collectors.toList());
	}

	public List<McWorksheetResults> getHighscoresForWorksheet(McWorksheet worksheet) {
		// get all students that completed this worksheet
		List<Integer> studentIds = em.createQuery(
				"SELECT id FROM Student " +
				"WHERE FUNCTION('has_student_completed_mc_worksheet', id, :mcWorksheetId) = TRUE", Integer.class)
				.setParameter("mcWorksheetId", worksheet.getMcWorksheetId())
				.getResultList();

		// get the results
		List<McWorksheetResults> resultsList = studentIds.stream()
				.map(studentId -> getStudentMcWorksheetResults(worksheet, studentId))
				.collect(Collectors.toList());

		// sort by points
		resultsList.sort((r1,r2) -> r2.getPoints() - r1.getPoints());

		return resultsList;
	}

	public List<McWorksheetResults> getLatestPublicStudentMcWorksheetsResults(int studentId, int courseTermId) {
		List<StudentMcWorksheet> worksheets = em.createQuery(
				"FROM StudentMcWorksheet " +
				"WHERE courseTerm.id = :courseTermId " +
				"AND student.id != :studentId " +
				"AND isPublic = TRUE " +
				"AND FUNCTION('has_student_completed_mc_worksheet', student.id, id) = TRUE " + // only completed worksheets
				"AND FUNCTION('has_student_completed_mc_worksheet', :studentId, id) = FALSE " + // no worksheets that we already completed
				"ORDER BY createdAt DESC", StudentMcWorksheet.class)
				.setParameter("courseTermId", courseTermId)
				.setParameter("studentId", studentId)
				.setMaxResults(LATEST_PUBLIC_WORKSHEETS_LIMIT)
				.getResultList();

		return worksheets.stream()
				.map(worksheet -> getStudentMcWorksheetResults(worksheet, worksheet.getStudent().getId()))
				.collect(Collectors.toList());
	}

	public McWorksheetResults getStudentMcWorksheetResults(McWorksheet mcWorksheet, int studentId) {
		List<McStatistic> mcStatistics = em.createQuery(
				"FROM McStatistic " +
				"WHERE student.id = :studentId AND mcWorksheetId = :mcWorksheetId " +
				"AND completedAt IS NOT NULL " +
				"ORDER BY questionOnWorksheet.number", McStatistic.class)
				.setParameter("studentId", studentId)
				.setParameter("mcWorksheetId", mcWorksheet.getMcWorksheetId())
				.getResultList();

		return new McWorksheetResults(mcWorksheet, mcStatistics);
	}

	public List<McStatistic> getForQuestionRoot(int mcQuestionRootId) {
		return em.createQuery(
				"FROM McStatistic WHERE completedAt IS NOT NULL " +
				"AND mcQuestionRootId = :mcQuestionRootId " +
				"ORDER BY completedAt DESC", McStatistic.class)
				.setParameter("mcQuestionRootId", mcQuestionRootId)
				.getResultList();
	}

	public List<McStatistic> getForQuestion(int mcQuestionId) {
		return em.createQuery(
				"FROM McStatistic WHERE completedAt IS NOT NULL " +
				"AND mc_Question_Id = :mcQuestionId " +
				"ORDER BY completedAt DESC", McStatistic.class)
				.setParameter("mcQuestionId", mcQuestionId)
				.getResultList();
	}

	public Map<InstructorMcWorksheet, Boolean> getInstructorMcWorksheetIsCompletedMap(int studentId, List<InstructorMcWorksheet> mcWorksheets) {
		Map<InstructorMcWorksheet,Boolean> map = new HashMap<>();

		for (InstructorMcWorksheet w : mcWorksheets) {
			boolean completed = jdbc.queryForObject(
					"SELECT * FROM has_student_completed_mc_worksheet(?,?)",
					new Object[]{studentId, w.getMcWorksheetId()}, Boolean.class);

			map.put(w, completed);
		}

		return map;
	}

	public List<McStatistic> getForQuestionOnWorksheet(int mcWorksheetToQuestionId) {
		return em.createQuery(
				"FROM McStatistic WHERE completedAt IS NOT NULL " +
				"AND questionOnWorksheet.id = :mcWorksheetToQuestionId",
				McStatistic.class)
				.setParameter("mcWorksheetToQuestionId", mcWorksheetToQuestionId)
				.getResultList();
	}
}
