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

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import de.whs.poodle.beans.forms.CreateMcWorksheetForm;
import de.whs.poodle.beans.mc.InstructorMcWorksheet;
import de.whs.poodle.beans.mc.McQuestionOnWorksheet;
import de.whs.poodle.beans.mc.McStudentCourseTermStatistics;
import de.whs.poodle.beans.mc.McWorksheet;
import de.whs.poodle.beans.mc.StudentMcWorksheet;


@Repository
public class McWorksheetRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@PersistenceContext
	private EntityManager em;

	/* returns the McWorksheet, regardless of whether it is an
	  StudentMcWorksheet or InstructorMcWorksheet. null if not found. */
	public McWorksheet getByMcWorksheetId(int mcWorksheetId) {
		// first check if a StudentMcWorksheet with this ID exists
		 McWorksheet mcWorksheet = em.find(StudentMcWorksheet.class, mcWorksheetId);

		if (mcWorksheet != null)
			return mcWorksheet;

		/* nope, must be an InstructorMcWorksheet (note that we can't use find() here since
		 * the ID of an InstructorMcWorksheet is linked to the other parent (Worksheet.java)) */
		try {
			return em.createQuery("FROM InstructorMcWorksheet WHERE mcWorksheetId = :mcWorksheetId", InstructorMcWorksheet.class)
				.setParameter("mcWorksheetId", mcWorksheetId)
				.getSingleResult();
		} catch(NoResultException e) {
			return null;
		}
	}

	public McQuestionOnWorksheet getQuestionOnWorksheetById(int mcWorksheetToQuestionId) {
		return em.find(McQuestionOnWorksheet.class, mcWorksheetToQuestionId);
	}


	public int createMcWorksheet(CreateMcWorksheetForm form, int studentId, int courseTermId) {
		return jdbc.query(
			con -> {
				PreparedStatement ps = con.prepareStatement("SELECT * FROM generate_student_mc_worksheet(?,?,?,?,?)");
				ps.setInt(1, courseTermId);
				ps.setInt(2, studentId);

				Array tagsArray = con.createArrayOf("int4", ObjectUtils.toObjectArray(form.getTags()));
				ps.setArray(3, tagsArray);

				ps.setInt(4, form.getMaximum());
				ps.setBoolean(5, form.isIgnoreAlreadyAnswered());

				return ps;
			},
			new ResultSetExtractor<Integer>() {

				@Override
				public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
					if (!rs.next()) // no results -> generated worksheet had no questions
						return 0;

					return rs.getInt("id");
				}
			}
		);
	}

	/*
	 * Returns how many questions a mcWorksheet generated with the specified criteria would have.
	 * This is used to show the number of questions on the "create worksheet" button so the student
	 * knows beforehand how many would be on the worksheet.
	 */
	public int getCountForMcWorksheet(CreateMcWorksheetForm form, int studentId, int courseTermId) {
		return jdbc.query(
			con -> {
				PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) AS count FROM get_mc_questions_for_worksheet(?,?,?,?,?)");
				ps.setInt(1, courseTermId);
				ps.setInt(2, studentId);

				Array tagsArray = con.createArrayOf("int4", ObjectUtils.toObjectArray(form.getTags()));
				ps.setArray(3, tagsArray);

				ps.setInt(4, form.getMaximum());
				ps.setBoolean(5, form.isIgnoreAlreadyAnswered());
				return ps;
			},
			new ResultSetExtractor<Integer>() {

				@Override
				public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
					rs.next();
					return rs.getInt("count");
				}

			}
		);
	}

	public McStudentCourseTermStatistics getMcStudentStatistics(int studentId, int courseTermId) {
		return jdbc.query(
			"SELECT SUM(CASE WHEN has_student_answered_mc_question(?,root_id) THEN 1 END)::INT AS answered, " +
			"COUNT(*) AS total " +
			"FROM v_mc_question " +
			"WHERE visibility != 'PRIVATE'" +
			"AND is_latest_revision " +
			"AND is_course_linked_with(" +
			"  (SELECT course_id FROM course_term sm WHERE sm.id = ?)," +
			"  course_id" +
			")",
			new Object[]{studentId,courseTermId},

			new ResultSetExtractor<McStudentCourseTermStatistics>() {

				@Override
				public McStudentCourseTermStatistics extractData(ResultSet rs) throws SQLException, DataAccessException {
					if (!rs.next())
						return null;

					McStudentCourseTermStatistics mcs = new McStudentCourseTermStatistics();

					mcs.setAnsweredCount(rs.getInt("answered"));
					mcs.setTotalCount(rs.getInt("total"));

					return mcs;
				}
			});
	}

	/*
	 * Called when a student has to answer an mc question. Creates the entry in mc_statistic so we
	 * have a timestamp for "seen-at".
	 */
	public void prepareMcStatistics(int studentId, int courseTermId, int mcWorksheetToQuestionId) {
		jdbc.update(
			con -> {
				CallableStatement cs = con.prepareCall("{ CALL prepare_mc_statistic(?,?,?) }");
				cs.setInt(1, studentId);
				cs.setInt(2, courseTermId);
				cs.setInt(3, mcWorksheetToQuestionId);
				return cs;
			});
	}

	@Transactional
	public void answerQuestion(int studentId, int mcWorksheetToQuestionId, List<Integer> answers) {
		int mcStatisticId = jdbc.queryForObject(
				"UPDATE mc_statistic SET completed_at = NOW() " +
				"WHERE student_id = ? " +
				"AND mc_worksheet_to_question_id = ? " +
				"RETURNING id",
				new Object[]{studentId, mcWorksheetToQuestionId},
				Integer.class);

		for (Integer answerId : answers) {
			jdbc.update("INSERT INTO mc_chosen_answer(mc_statistic_id,mc_answer_id) VALUES(?,?)",
					mcStatisticId, answerId);
		}
	}

	public void cancelWorksheet(int studentId, int courseTermId) {
		Integer mcWorksheetId = getCurrentGeneratedMcWorksheetId(studentId, courseTermId);

		jdbc.update(
			"UPDATE student_to_course_term SET student_mc_worksheet_id = NULL " +
			"WHERE student_id = ? AND course_term_id = ?",
			studentId, courseTermId);

		if (mcWorksheetId != null) {
			Boolean isPublic = jdbc.queryForObject(
					"SELECT is_public FROM student_mc_worksheet WHERE id = ?",
					new Object[]{mcWorksheetId}, Boolean.class);

			if (!isPublic)
				jdbc.update("DELETE FROM mc_worksheet WHERE id = ?", mcWorksheetId);
		}
	}

	/*
	 * We pass the studentId here as well so a student can only make his own worksheets
	 * public (a fake POST request could be made otherwise).
	 */
	public void setWorksheetPublic(int studentId, int mcWorksheetId){
		jdbc.update("UPDATE student_mc_worksheet SET is_public = TRUE WHERE student_id = ? AND id = ?",
				studentId, mcWorksheetId);
	}

	public boolean canStudentSetMcWorksheetPublic(int studentId, int mcWorksheetId) {
		try {
			return jdbc.queryForObject(
				"SELECT NOT is_public FROM student_mc_worksheet WHERE student_id = ? AND id = ?",
				new Object[]{studentId, mcWorksheetId},
				Boolean.class);
		} catch(EmptyResultDataAccessException e) {
			return false;
		}
	}

	public void setWorksheetForStudent(int studentId, int courseTermId, int mcWorksheetId) {
		jdbc.update(
			"UPDATE student_to_course_term SET student_mc_worksheet_id = ? " +
			"WHERE course_term_id = ? AND student_id = ?",
			mcWorksheetId, courseTermId, studentId);
	}

	/*
	 * Returns the mc_worksheet_to_question id of the question that the student
	 * must answer next on the worksheet, NULL if the worksheet has already been completed.
	 *
	 * The next question is simply the first question on the worksheet for which no
	 * "completed_at timestamp exists in mc_statistic.
	 */
	public Integer getCurrentQuestionIdForStudentAndWorksheet(int studentId, int mcWorksheetId) {
		try {
			return jdbc.queryForObject(
					"SELECT id FROM mc_worksheet_to_question " +
					"WHERE mc_worksheet_id = ? " +
					"AND NOT EXISTS (" +
					" SELECT 1 FROM mc_statistic WHERE student_id = ? " +
					" AND mc_worksheet_to_question_id = mc_worksheet_to_question.id " +
					" AND completed_at IS NOT NULL " +
					")" +
					"ORDER BY number LIMIT 1",
					new Object[]{mcWorksheetId, studentId},
					Integer.class);
		} catch(IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	/*
	 * Get the ID of the generated MC worksheet that the student is currently
	 * working on for this course term. NULL if none is set or the worksheet
	 * has already been completed.
	 */
	public Integer getCurrentGeneratedMcWorksheetId(int studentId, int courseTermId) {
		try {
			return jdbc.queryForObject(
					"SELECT student_mc_worksheet_id FROM student_to_course_term " +
					"WHERE student_id = ? AND course_term_id = ? " +
					"AND student_mc_worksheet_id IS NOT NULL " +
					"AND NOT has_student_completed_mc_worksheet(student_id,student_mc_worksheet_id)",
					new Object[]{studentId, courseTermId},
					Integer.class);
		} catch(IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	public StudentMcWorksheet getStudentMcWorksheetById(int id) {
		return em.find(StudentMcWorksheet.class, id);
	}
}