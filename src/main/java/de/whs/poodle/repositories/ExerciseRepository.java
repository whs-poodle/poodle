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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
import de.whs.poodle.beans.Exercise;
import de.whs.poodle.beans.Exercise.SampleSolutionType;
import de.whs.poodle.beans.ExerciseSearchCriteria;
import de.whs.poodle.beans.ExerciseSearchResult;
import de.whs.poodle.beans.Tag;
import de.whs.poodle.repositories.exceptions.BadRequestException;
import de.whs.poodle.repositories.exceptions.ForbiddenException;

@Repository
public class ExerciseRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@PersistenceContext
	private EntityManager em;


	/*
	 * Save an exercise in the database. Note that due to the revisions, we
	 * always create a completely new exercise in the database. The root ID
	 * defines whether this is a new exercise or a new revision of an existing one.
	 */
	public void save(Exercise exercise) {
		if (exercise.getTitle().trim().isEmpty())
			throw new BadRequestException("noTitleSpecified");
		if (exercise.getText().trim().isEmpty())
			throw new BadRequestException("noExerciseTextSpecified");

		jdbc.execute(
				new ConnectionCallback<Void>() {

					@Override
					public Void doInConnection(Connection con) throws SQLException, DataAccessException {
						try (
							CallableStatement exercisePs = con.prepareCall(
									"{ ? = CALL create_exercise(?,?,?::exercise_visibility,?,?,?,?,?,?,?,?,?) }");

							PreparedStatement tagsPs = con.prepareStatement(
									"INSERT INTO exercise_to_tag(exercise_id,tag_id) VALUES(?,?)");
						) {
							con.setAutoCommit(false);

							// inner try for rollback
							try {
								// create exercise
								exercisePs.registerOutParameter(1, Types.INTEGER); // new_id

								exercisePs.setString(2, exercise.getText());

								/*
								 * The root id is always the ID of the first revision. If this
								 * is a new exercise, this ID obviously doesn't exist yet. We set
								 * NULL in this case, but a trigger in the DB will automatically
								 * set the root_id to the generated id.
								 */
								if (exercise.getRootId() == 0)
									exercisePs.setNull(3, Types.INTEGER);
								else
									exercisePs.setInt(3, exercise.getRootId());

								exercisePs.setString(4, exercise.getVisibility().toString());
								exercisePs.setString(5, exercise.getTitle());
								exercisePs.setInt(6, exercise.getChangedBy().getId());
								exercisePs.setString(7, exercise.getHint1());
								exercisePs.setString(8, exercise.getHint2());

								// sample solution
								SampleSolutionType sampleSolutionType = exercise.getSampleSolutionType();

								if (sampleSolutionType == SampleSolutionType.NONE) {
									exercisePs.setNull(9, Types.INTEGER);
									exercisePs.setNull(10, Types.VARCHAR);
								}
								else if (sampleSolutionType == SampleSolutionType.FILE) {
									exercisePs.setInt(9, exercise.getSampleSolution().getFile().getId());
									exercisePs.setNull(10, Types.VARCHAR);
								}
								else { // must be text
									exercisePs.setNull(9, Types.INTEGER);
									exercisePs.setString(10, exercise.getSampleSolution().getText());
								}

								// attachments
								List<Integer> attachmentIds = exercise.getAttachments().stream()
										.map(a -> a.getId())
										.collect(Collectors.toList());

								Array anhaengeIdsArray = con.createArrayOf("int4", attachmentIds.toArray());
								exercisePs.setArray(11, anhaengeIdsArray);

								exercisePs.setInt(12, exercise.getCourseId());

								exercisePs.setString(13, exercise.getComment());

								exercisePs.executeUpdate();

								/* Set the generated ID so the calling function can read it. */
								exercise.setId(exercisePs.getInt(1));

								// create relation to tags
								tagsPs.setInt(1, exercise.getId());

								for (Tag t : exercise.getTags()) {
									tagsPs.setInt(2, t.getId());
									tagsPs.addBatch();
								}

								tagsPs.executeBatch();

								con.commit();
							} catch(SQLException e) {
								con.rollback();
								throw e;
							} finally {
								con.setAutoCommit(true);
							}
						}

						return null;
					}
				}
		);
	}

	public Exercise getById(int id) {
		return em.find(Exercise.class, id);
	}

	public Exercise getLatestForRootId(int rootId) {
		try {
			return em.createQuery(
					"FROM Exercise WHERE rootId = :rootId AND latestRevision = TRUE", Exercise.class)
					.setParameter("rootId", rootId)
					.getSingleResult();
		}catch (NoResultException e) {
			return null;
		}
	}

	public List<Exercise> getAllRevisionsForRoot(int exerciseRootId) {
		return em.createQuery(
				"FROM Exercise WHERE rootId = :rootId ORDER BY createdAt DESC", Exercise.class)
				.setParameter("rootId", exerciseRootId)
				.getResultList();
	}

	/*
	 * delete the exercise and all its revisions.
	 */
	public void delete(int exerciseId) {
		try {
			jdbc.update(
				"DELETE FROM exercise WHERE root_id = (SELECT root_id FROM exercise a WHERE a.id = ?)",
				exerciseId);
		} catch(DataIntegrityViolationException e) {
			throw new ForbiddenException();
		}
	}

	public List<Exercise> getLatestExercises(int instructorId, int limit) {
		return em.createQuery(
				"FROM Exercise WHERE FUNCTION('has_instructor_access_to_course', courseId, :instructorId) = TRUE " +
				"ORDER BY createdAt DESC", Exercise.class)
				.setParameter("instructorId", instructorId)
				.setMaxResults(limit)
				.getResultList();
	}

	/*
	 * Returns all exercises matching the passed ExerciseSearchCriteria. The text search is done in Java instead of the database
	 * since searching HTML code would be involve a complex and error prone regex (due to the HTML tags and the escaping etc.). Instead, we
	 * convert the exercise text into plain text via JSoup and then search it via Java.
	 *
	 * see ExerciseSearchCriteria for details regarding the criteria.
	 */
	public List<ExerciseSearchResult> search(ExerciseSearchCriteria s) {
		String query =
				"SELECT * FROM search_exercises(" +
						"CAST(:courses AS INT[])," +
						"CAST(:tags AS INT[])," +
						"CAST(:instructors AS INT[])," +
						"CAST(:difficultyMode AS difficulty_mode)," +
						":difficulty," +
						(s.getWorksheetFilter() == null ? "NULL" : ":worksheetFilter") + ",";

		// student specific filters
		if (s.isStudent())
			query += ":studentId,:courseTermId,:hideIfAlreadyCompleted,";
		else
			query += "NULL,NULL,NULL,";

		query += ":withFeedback,";

		// instructor id (used to filter exercises from courses that the instructor has no access to
		if (s.getInstructorId() == null)
			query += "NULL,";
		else
			query += ":instructorId,";

		query += ":tagsAnd) " +
				"ORDER BY " + s.getOrder().getDbString() + " " + (s.isOrderAscending() ? "ASC" : "DESC") + " NULLS LAST";

		// set the parameters
		Query emQuery = em.createNativeQuery(query, ExerciseSearchResult.class)
				.setParameter("courses", Utils.idsToPsqlArrayString(s.getCourses()))
				.setParameter("tags", Utils.idsToPsqlArrayString(s.getTags()))
				.setParameter("instructors", Utils.idsToPsqlArrayString(s.getInstructors()))
				.setParameter("difficultyMode", s.getDifficultyMode().name())
				.setParameter("difficulty", s.getDifficulty())
				.setParameter("tagsAnd", s.isTagsAnd())
				.setParameter("withFeedback", s.isWithFeedback());

		if (s.getWorksheetFilter() != null)
			emQuery.setParameter("worksheetFilter", s.getWorksheetFilter());

		if (s.isStudent()) {
			emQuery
				.setParameter("studentId", s.getStudentFilter().getId())
				.setParameter("courseTermId", s.getStudentFilter().getCourseTermId())
				.setParameter("hideIfAlreadyCompleted", s.getStudentFilter().isHideIfAlreadyCompleted());
		}

		if (s.getInstructorId() != null)
			emQuery.setParameter("instructorId", s.getInstructorId());

		// finally execute the query
		@SuppressWarnings("unchecked")
		List<ExerciseSearchResult> exercises = emQuery.getResultList();

		// filter by text, if necessary
		String text = s.getText().toLowerCase();
		boolean searchTitle = s.isSearchTitle();
		boolean searchText = s.isSearchText();

		if (text.isEmpty() || (!searchTitle && !searchText)) {
			// no text search, return the list
			return exercises;
		}

		// remove exercises that don't match the text
		Iterator<ExerciseSearchResult> it = exercises.iterator();

		while (it.hasNext()) {
			ExerciseSearchResult exercise = it.next();
			String title = exercise.getTitle().toLowerCase();
			String exerciseText = Jsoup.parse(exercise.getText()).text().toLowerCase(); // HTML -> plain text

			if (searchTitle && searchText &&					// title and text
				!title.contains(text)  && !exerciseText.contains(text) ||
				searchTitle && !searchText &&					// title only
				!title.contains(text) ||
				!searchTitle && searchText &&					// text only
				!exerciseText.contains(text)) {
				it.remove();
			}
		}

		return exercises;
	}
}
