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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.whs.poodle.beans.mc.InstructorMcWorksheet;
import de.whs.poodle.beans.mc.McQuestionOnWorksheet;

/*
 * Contains all functions that are specific to InstructorMcWorksheets. Note
 * that a lot of functions shared between both Worksheet types are
 * in WorksheetRepository.
 */
@Repository
public class InstructorMcWorksheetRepository {

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;

	public InstructorMcWorksheet getById(int id) {
		return em.find(InstructorMcWorksheet.class, id);
	}

	public InstructorMcWorksheet getByMcWorksheetId(int mcWorksheetId) {
		int id = jdbc.queryForObject(
				"SELECT id FROM instructor_mc_worksheet WHERE mc_worksheet_id = ?",
				new Object[]{mcWorksheetId}, Integer.class);
		return em.find(InstructorMcWorksheet.class, id);
	}

	public List<InstructorMcWorksheet> getForCourseTerm(int courseTermId) {
		return em.createQuery(
				"FROM InstructorMcWorksheet ws " +
				"WHERE ws.courseTerm.id = :courseTermId " +
				"ORDER BY ws.number", InstructorMcWorksheet.class)
				.setParameter("courseTermId", courseTermId)
				.getResultList();
	}

	public void addQuestion(int mcWorksheetId, int mcQuestionId) {
		int number = jdbc.queryForObject(
				"SELECT COUNT(*)+1 FROM mc_worksheet_to_question WHERE mc_worksheet_id = ?",
				new Object[]{mcWorksheetId}, Integer.class);

		jdbc.update("INSERT INTO mc_worksheet_to_question(number,mc_worksheet_id,mc_question_id) VALUES(?,?,?)",
				number, mcWorksheetId, mcQuestionId);
	}

	public void removeQuestion(int mcWorksheetToQuestionId) {
		jdbc.update("DELETE FROM mc_worksheet_to_question WHERE id = ?", mcWorksheetToQuestionId);
	}

	@Transactional
	public void moveQuestion(int mcWorksheetToQuestionId, boolean up) {
		McQuestionOnWorksheet question = em.find(McQuestionOnWorksheet.class, mcWorksheetToQuestionId);

		int number = question.getNumber();
		int otherNumber = number + (up ? -1 : 1);
		int mcWorksheetId = question.getMcWorksheetId();

		// get the other question that we swap with
		McQuestionOnWorksheet otherQuestion;
		try {
			otherQuestion = em.createQuery(
				"FROM McQuestionOnWorksheet WHERE mcWorksheetId = :mcWorksheetId " +
				"AND number = :number", McQuestionOnWorksheet.class)
				.setParameter("mcWorksheetId", mcWorksheetId)
				.setParameter("number", otherNumber)
				.getSingleResult();
		} catch(NoResultException e) {
			// question is already on top/bottom
			return;
		}

		// swap the numbers in both questions
		em.createNativeQuery(
				"UPDATE mc_worksheet_to_question AS q1 " +
				"SET number = q2.number " +
				"FROM mc_worksheet_to_question q2 " +
				"WHERE q1.id IN (:id1,:id2) AND q2.id IN (:id1,:id2) AND q1.id != q2.id")
				.setParameter("id1", question.getId())
				.setParameter("id2", otherQuestion.getId())
				.executeUpdate();
	}
}
