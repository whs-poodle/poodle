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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import de.whs.poodle.Utils;
import de.whs.poodle.beans.Tag;
import de.whs.poodle.beans.mc.McQuestion;
import de.whs.poodle.beans.mc.McQuestion.Answer;
import de.whs.poodle.beans.mc.McQuestionSearchCriteria;
import de.whs.poodle.repositories.exceptions.BadRequestException;
import de.whs.poodle.repositories.exceptions.ForbiddenException;

@Repository
public class McQuestionRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@PersistenceContext
	private EntityManager em;

	public int save(McQuestion question) {

		if (question.getText().trim().isEmpty())
			throw new BadRequestException("noExerciseTextSpecified");

		for (Answer a : question.getAnswers()) {
			if (a.getText().trim().isEmpty())
				throw new BadRequestException("emptyAnswer");
		}

		return jdbc.execute(
			new ConnectionCallback<Integer>() {

				@Override
				public Integer doInConnection(Connection con) throws SQLException, DataAccessException {
					try (
						PreparedStatement questionPs = con.prepareStatement(
								"INSERT INTO mc_question(course_id,text,has_multiple_correct_answers,changed_by_id,root_id,visibility) VALUES(?,?,?,?,?,?::exercise_visibility)",
								PreparedStatement.RETURN_GENERATED_KEYS);

						PreparedStatement answersPs = con.prepareStatement(
								"INSERT INTO mc_question_to_answer(mc_question_id,correct,text) VALUES(?,?,?)");

						PreparedStatement tagsPs = con.prepareStatement(
								"INSERT INTO mc_question_to_tag(mc_question_id,tag_id) VALUES(?,?)");

						/* If this is a new revision, this updates the revision in existing worksheets.
						 * Note that we only do this if no statistics exist yet, because we would screw
						 * those up otherwise (the answer IDs in mc_chosen_answer wouldn't match the
						 * question revision anymore). */
						PreparedStatement updateWorksheetsPs = con.prepareStatement(
								"UPDATE mc_worksheet_to_question wtq SET mc_question_id = ? " +
								"WHERE mc_question_id IN (SELECT id FROM mc_question WHERE root_id = ?) " +
								"AND NOT EXISTS (SELECT 1 FROM mc_statistic WHERE mc_worksheet_to_question_id = wtq.id)");
					) {
						con.setAutoCommit(false);

						// inner try for rollback
						try {
							// exercise
							questionPs.setInt(1, question.getCourseId());
							questionPs.setString(2,	 question.getText());
							questionPs.setBoolean(3, question.isMultipleCorrectAnswers());

							questionPs.setInt(4, question.getChangedBy().getId());

							/*
							 * The root id is always the ID of the first revision. If this
							 * is a new exercise, this ID obviously doesn't exist yet. We set
							 * NULL in this case, but a trigger in the DB will automatically
							 * set the root_id to the generated id.
							 */
							if (question.getRootId() == 0)
								questionPs.setNull(5, Types.INTEGER);
							else
								questionPs.setInt(5, question.getRootId());

							questionPs.setString(6, question.getVisibility().toString());

							questionPs.executeUpdate();

							ResultSet genRs = questionPs.getGeneratedKeys();
							genRs.next();
							int questionId = genRs.getInt(1);

							// answers
							answersPs.setInt(1, questionId);

							for (Answer a : question.getAnswers()) {
								answersPs.setBoolean(2, a.isCorrect());
								answersPs.setString(3, a.getText());
								answersPs.addBatch();
							}

							answersPs.executeBatch();

							// tag relations
							tagsPs.setInt(1, questionId);

							for (Tag t : question.getTags()) {
								tagsPs.setInt(2, t.getId());
								tagsPs.addBatch();
							}

							tagsPs.executeBatch();

							// if this is new revision, update it in the worksheets
							if (question.getRootId() != 0) {
								updateWorksheetsPs.setInt(1, questionId);
								updateWorksheetsPs.setInt(2, question.getRootId());
								updateWorksheetsPs.executeUpdate();
							}

							con.commit();

							return questionId;

						} catch(SQLException e) {
							con.rollback();
							throw e;
						} finally {
							con.setAutoCommit(true);
						}
					}
				}
			});
	}

	public McQuestion getById(int id) {
		return em.find(McQuestion.class, id);
	}


	public void delete(int mcQuestionId) {
		try {
			jdbc.update(
				"DELETE FROM mc_question WHERE root_id = (SELECT root_id FROM mc_question a WHERE a.id = ?)",
				mcQuestionId);
		} catch(DataIntegrityViolationException e) {
			throw new ForbiddenException();
		}
	}

	public List<McQuestion> getAllRevisionsForRoot(int mcQuestionRootId) {
		return em.createQuery(
				"FROM McQuestion WHERE rootId = :rootId ORDER BY createdAt DESC", McQuestion.class)
				.setParameter("rootId", mcQuestionRootId)
				.getResultList();
	}

	public boolean hasCoursePublicMcQuestions(int courseId) {
		return jdbc.queryForObject(
			"SELECT EXISTS (SELECT 1 FROM v_mc_question " +
			"WHERE is_course_linked_with(?,v_mc_question.course_id) " + // check exercise from this course and all linked courses
			"AND visibility = 'PUBLIC'" +
			"AND is_latest_revision)",
			new Object[]{courseId}, Boolean.class);
	}

	public List<McQuestion> getLatest(int instructorId, int limit) {
		return em.createQuery(
				"FROM McQuestion " +
				"WHERE FUNCTION('has_instructor_access_to_course', courseId, :instructorId) = TRUE " +
				"ORDER BY createdAt DESC", McQuestion.class)
				.setParameter("instructorId", instructorId)
				.setMaxResults(limit)
				.getResultList();
	}

	// works similiar to ExerciseRepository.search()
	public List<McQuestion> search(McQuestionSearchCriteria s) {
		/*
		 * instructorId and filterInstructorMcWorksheetId
		 * can be null. Since JPA is stupid and sets the database
		 * type to BYTEA if the value passed to setParameter() is null,
		 * we have to check this ourselves and set NULL explicitly.
		 */
		String query =
				"SELECT * FROM search_mc_questions(" +
					"CAST(:courses AS INT[])," +
					"CAST(:tags AS INT[])," +
					"CAST(:instructors AS INT[])," +
					(s.getInstructorId() == null ? "NULL" : ":instructorId") + "," +
					":tagsAnd," +
					(s.getFilterInstructorMcWorksheetId() == null ? "NULL" : ":instructorMcWorksheetId") +
				") ORDER BY " + s.getOrder().getDbString() + " " + (s.isOrderAscending() ? "ASC" : "DESC");


		Query emQuery = em.createNativeQuery(query, McQuestion.class)
			.setParameter("courses", Utils.idsToPsqlArrayString(s.getCourses()))
			.setParameter("tags", Utils.idsToPsqlArrayString(s.getTags()))
			.setParameter("instructors", Utils.idsToPsqlArrayString(s.getInstructors()))
			.setParameter("tagsAnd", s.isTagsAnd());

		if (s.getInstructorId() != null)
			emQuery.setParameter("instructorId", s.getInstructorId());

		if (s.getFilterInstructorMcWorksheetId() != null)
			emQuery.setParameter("instructorMcWorksheetId", s.getFilterInstructorMcWorksheetId());

		@SuppressWarnings("unchecked")
		List<McQuestion> questions = emQuery.getResultList();

		// no text search
		if (s.getText().isEmpty())
			return questions;

		// remove exercises not matching the text
		String searchText = s.getText().toLowerCase();

		Iterator<McQuestion> it = questions.iterator();
		while (it.hasNext()) {
			String questionText = it.next().getText();
			String questionTextPlain = Jsoup.parse(questionText).text().toLowerCase();

			if (!questionTextPlain.contains(searchText))
				it.remove();
		}

		return questions;
	}
}
