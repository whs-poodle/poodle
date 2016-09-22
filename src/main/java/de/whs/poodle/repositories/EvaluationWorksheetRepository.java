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

import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.whs.poodle.beans.Worksheet.WorksheetType;
import de.whs.poodle.beans.evaluation.EvaluationQuestion;
import de.whs.poodle.beans.evaluation.EvaluationQuestion.EvaluationQuestionType;
import de.whs.poodle.beans.evaluation.EvaluationQuestionChoice;
import de.whs.poodle.beans.evaluation.EvaluationSection;
import de.whs.poodle.beans.evaluation.EvaluationStatistic;
import de.whs.poodle.beans.evaluation.EvaluationWorksheet;
import de.whs.poodle.beans.evaluation.StudentEvaluationData;
import de.whs.poodle.beans.evaluation.StudentEvaluationData.StudentEvQuestionChoice;

@Repository
public class EvaluationWorksheetRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@PersistenceContext
	private EntityManager em;

	public EvaluationWorksheet getById(int id) {
		return em.find(EvaluationWorksheet.class, id);
	}

	public EvaluationQuestion getQuestionById(int evaluationQuestionId) {
		return em.find(EvaluationQuestion.class, evaluationQuestionId);
	}

	// note that one course term can only ever have a single evaluation worksheet
	public EvaluationWorksheet getForCourseTerm(int courseTermId) {
		try {
			return em.createQuery("FROM EvaluationWorksheet WHERE courseTerm.id = :courseTermId", EvaluationWorksheet.class)
					.setParameter("courseTermId", courseTermId)
					.getSingleResult();
		} catch(NoResultException e) {
			// this course term doesn't have an evaluation
			return null;
		}
	}

	/*
	 * Returns a map, mapping each text(!) question in the evaluation worksheet
	 * to the texts provided by the student.
	 */
	public Map<EvaluationQuestion,List<String>> getQuestionToTextsMapForEvaluation(int evaluationWorksheetId) {
		List<EvaluationStatistic> stats = em.createQuery(
				"FROM EvaluationStatistic WHERE question.section.worksheet.id = :worksheetId " +
				"AND text IS NOT NULL", EvaluationStatistic.class)
				.setParameter("worksheetId", evaluationWorksheetId)
				.getResultList();

		return stats.stream().collect(
				// group the list...
				Collectors.groupingBy(
					// by question
					EvaluationStatistic::getQuestion,
					// and map each question to the list of texts
					Collectors.mapping(EvaluationStatistic::getText, Collectors.toList())
				)
			);
	}

	public List<EvaluationStatistic> getStatisticsForQuestion(int evaluationQuestionId) {
		return em.createQuery(
				"FROM EvaluationStatistic WHERE question.id = :questionId",
				EvaluationStatistic.class)
				.setParameter("questionId", evaluationQuestionId)
				.getResultList();
	}


	public Map<EvaluationWorksheet, Boolean> getEvaluationIsCompletedMap(int studentId, List<EvaluationWorksheet> allEvaluationWorksheets) {
		Map<EvaluationWorksheet,Boolean> map = new HashMap<>();

		for (EvaluationWorksheet ws : allEvaluationWorksheets) {
			/* It is enough to check for count > 0 here since we write all statistics that
			 * a student created for an evaluation into the database at once, so if 1 exists, all exist. */
			boolean completed = em.createQuery(
					"SELECT COUNT(stats) > 0 FROM EvaluationStatistic stats " +
					"WHERE stats.student.id = :studentId AND question.section.worksheet.id = :worksheetId", Boolean.class)
					.setParameter("studentId", studentId)
					.setParameter("worksheetId", ws.getId())
					.getSingleResult();

			map.put(ws, completed);
		}

		return map;
	}


	@Transactional
	public void saveStudentEvaluation(int worksheetId, int studentId, StudentEvaluationData data) {
		EvaluationWorksheet worksheet = getById(worksheetId);

		// iterate all questions in the worksheet and make sure the data matches them
		for (EvaluationSection section : worksheet.getSections()) {
			for (EvaluationQuestion question : section.getQuestions()) {

				// get the choice for this question
				StudentEvQuestionChoice choice = data.getQuestionChoices().stream()
						.filter(q -> q.getEvaluationQuestionId() == question.getId())
						.findAny()
						.orElse(null);

				if (choice == null)
					throw new RuntimeException("data contains no choice for question " + question.getId());

				if (question.getType() == EvaluationQuestionType.TEXT && choice.getChoiceId() != null)
					throw new RuntimeException("question " + question.getId() +
							" is a text question, but choice contained choiceId");

				if (question.getType() == EvaluationQuestionType.CHOICE && choice.getText() != null)
					throw new RuntimeException("question " + question.getId() + ""
							+ " is a choice question, but choice contained text");
			}
		}

		// data seems to be valid, write it to the database
		for (StudentEvQuestionChoice choice : data.getQuestionChoices()) {
			String text = choice.getText();
			// don't write empty strings into the db
			if (text != null && text.trim().isEmpty())
				text = null;

			jdbc.update(
					"INSERT INTO evaluation_statistic(student_id,evaluation_question_id,choice_id,text) " +
					"VALUES(?,?,?,?)",
					new Object[]{studentId, choice.getEvaluationQuestionId(), choice.getChoiceId(), text},
					new int[]{Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR}
			);
		}
	}

	@Transactional
	public int create(int courseTermId) {
		/* create the worksheet entry. Note that since there is only
		 * ever one evaluation per course term, we can simply hardcode
		 * the number and title here since they are not used anywhere anyway. */
		int worksheetId = jdbc.queryForObject(
				"INSERT INTO worksheet(course_term_id,number,title,type) VALUES(?,?,?,?::worksheet_type) RETURNING id",
				new Object[]{courseTermId, 1, "evaluation", WorksheetType.EVALUATION.name()},
				Integer.class);

		// create evaluation_worksheet entry
		jdbc.update("INSERT INTO evaluation_worksheet(id) VALUES(?)", worksheetId);

		return worksheetId;
	}

	public int addSection(int evaluationWorksheetId, String title, int number) {
		return jdbc.queryForObject("INSERT INTO evaluation_section(evaluation_worksheet_id,number,title) VALUES(?,?,?) RETURNING id",
				new Object[]{evaluationWorksheetId, number, title},
				Integer.class);
	}

	@Transactional
	public int addQuestionToSection(int evaluationSectionId, EvaluationQuestion question) {
		// question itself
		int questionId =jdbc.queryForObject(
				"INSERT INTO evaluation_question(evaluation_section_id,number,text,allow_not_applicable) VALUES(?,?,?,?) RETURNING id",
				new Object[]{evaluationSectionId, question.getNumber(), question.getText(), question.isAllowNotApplicable()},
				Integer.class);

		// choices (if any)
		for (EvaluationQuestionChoice choice : question.getChoices()) {
			jdbc.update(
					"INSERT INTO evaluation_question_to_choice(evaluation_question_id,value,text) VALUES(?,?,?)",
					new Object[]{questionId, choice.getValue(), choice.getText()},
					// must specify types since value and text may be null and it can't determine the type in this case
					new int[]{Types.INTEGER, Types.INTEGER, Types.VARCHAR});
		}

		return questionId;
	}

	@Transactional
	public void setUnlockTimes(int evaluationWsId, Date unlockAt, Date unlockedUntil) {
		jdbc.update(
					"UPDATE worksheet SET unlock_at = ? WHERE id = ? ",
					unlockAt, evaluationWsId
				);
		jdbc.update(
					"UPDATE evaluation_worksheet SET unlocked_until = ? WHERE id = ?",
					unlockedUntil, evaluationWsId
				);
	}
}
