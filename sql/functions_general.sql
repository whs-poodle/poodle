-- Copyright 2015 Westfälische Hochschule
--
-- This file is part of Poodle.
--
-- Poodle is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- Poodle is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with Poodle.  If not, see <http://www.gnu.org/licenses/>.



-- Returns whether _linked_course_id is a linked course of _course_id. This is e.g. used
-- to allow students access to exercises from the linked course.

CREATE OR REPLACE FUNCTION is_course_linked_with(_course_id INT, _linked_course_id INT) RETURNS BOOL AS $$
BEGIN
	RETURN _course_id = _linked_course_id OR
		_linked_course_id IN (SELECT linked_course_id FROM course_to_linked_course WHERE course_id = _course_id);
END $$ LANGUAGE plpgsql STABLE;



-- Exercise search function. The parameters match the ExerciseSearchCriteria class.

-- Only needed in search_exercises(), so define it here
DROP TYPE IF EXISTS difficulty_mode CASCADE;

CREATE TYPE difficulty_mode
	AS ENUM('NONE', 'MIN', 'MAX');

CREATE OR REPLACE FUNCTION search_exercises(
	_courses INT[],
	_tags INT[],
	_instructors INT[],
	_difficulty_mode difficulty_mode,
	_difficulty INT,
	_worksheet_filter INT,
	_student_id INT,
	_course_term_id INT,
	_hide_already_done BOOL,
	_with_feedback_only BOOL,
	_instructor_id INT,
	_tags_and BOOL
)
RETURNS SETOF v_exercise_search_result AS $$
DECLARE
	_tag_names TEXT[];
BEGIN
	-- Note that tags with same name can exist multiple times in different courses. However, in the
	-- search form a tag name is only ever shown once. So if we simply search by IDs, we might miss
	-- exercises which have the same tag name, but with a different tag ID.
	-- To avoid this, get all names for the passed IDs and search by those.
	_tag_names := ARRAY(SELECT name FROM tag WHERE id = ANY(_tags));

	RETURN QUERY
	WITH exercise_tags AS (
		SELECT name,exercise_id FROM tag
		JOIN exercise_to_tag ON exercise_to_tag.tag_id = tag.id
	)
	SELECT DISTINCT exercise.* FROM v_exercise_search_result AS exercise

	-- filter by tags
	WHERE (ARRAY_LENGTH(_tags, 1) IS NULL OR (
			-- AND search (@> = Contains All)
			_tags_and AND     ARRAY(SELECT name FROM exercise_tags WHERE exercise_id = exercise.id) @> _tag_names OR
			-- OR search (&& = Overlaps)
			NOT _tags_and AND ARRAY(SELECT name FROM exercise_tags WHERE exercise_id = exercise.id) && _tag_names
		)
	)
	-- filter by average difficulty
	AND (
		_difficulty_mode = 'NONE' OR
		_difficulty_mode = 'MIN' AND avg_difficulty >= _difficulty OR
		_difficulty_mode = 'MAX' AND avg_difficulty <= _difficulty
	)
	-- ignore exercises that already exist in one of the worksheets in the specified course term (this is only
	-- passed if the instructor is adding exercises to a worksheet from the search).
	AND (
		_worksheet_filter IS NULL OR
		NOT EXISTS (
			SELECT 1
			FROM exercise a2,worksheet,chapter,chapter_to_exercise
			WHERE worksheet.id = chapter.worksheet_id
			AND chapter_to_exercise.chapter_id = chapter.id
			AND chapter_to_exercise.exercise_id = a2.id
			AND exercise.id = a2.id
			AND worksheet.course_term_id = _worksheet_filter
		)
	)
	-- instructor specific filters
	AND (
		_instructor_id IS NULL OR  -- NULL if student search
		-- filter by owner
		(ARRAY_LENGTH(_instructors, 1) IS NULL OR owner_id = ANY(_instructors))
		-- only exercises from courses that the instructor has access to
		AND has_instructor_access_to_course(exercise.course_id,_instructor_id)
	)
	-- student specific filters (note that a student can only use the search to add exercises to a self study worksheet)
	AND (
		_student_id IS NULL OR
		-- ignore exercises that already exist on the worksheet
		NOT EXISTS (
			SELECT 1 FROM self_study_worksheet_to_exercise swe
			JOIN exercise e2 ON e2.id = swe.exercise_id
			JOIN student_to_course_term sct ON swe.student_to_course_term_id = sct.id
			WHERE exercise.root_id = e2.root_id
			AND student_id = _student_id
			AND course_term_id = _course_term_id
		)

		-- ignore exercises that the student already completed
		AND (
			NOT _hide_already_done OR
			NOT EXISTS (
				SELECT 1 FROM v_statistic s
				WHERE s.exercise_root_id = exercise.root_id AND student_id  = _student_id
			)
		)
		-- students can't see private exercises in the search
		AND visibility != 'PRIVATE'
	)
	AND (NOT _with_feedback_only OR has_feedback)
	-- filter by courses
	AND (ARRAY_LENGTH(_courses, 1) IS NULL OR exercise.course_id = ANY(_courses));

END $$ LANGUAGE plpgsql STABLE;



-- Function to search for mc questions. The parameters match the McQuestionSearchCriteria.
--  This works very similiar to search_exercises() so also check this for comments.

CREATE OR REPLACE FUNCTION search_mc_questions(
	_courses INT[],
	_tags INT[],
	_instructors INT[],
	_instructor_id INT,
	_tags_and BOOL,
	_filter_instructor_mc_worksheet_id INT
)
RETURNS SETOF v_mc_question AS $$
DECLARE
	_tag_names TEXT[];
BEGIN
	-- see comments in search_exercises()
	_tag_names := ARRAY(SELECT name FROM tag WHERE id = ANY(_tags));

	RETURN QUERY
	WITH mc_question_tags AS (
		SELECT name,mc_question_id FROM tag
		JOIN mc_question_to_tag ON mc_question_to_tag.tag_id = tag.id
	)
	SELECT DISTINCT mc_question.* FROM v_mc_question AS mc_question

	-- filter by courses
	WHERE (ARRAY_LENGTH(_courses, 1) IS NULL OR mc_question.course_id = ANY(_courses))

	-- filter by tags
	AND (ARRAY_LENGTH(_tags, 1) IS NULL OR (
			-- AND search (@> = Contains All)
			_tags_and AND     ARRAY(SELECT name FROM mc_question_tags WHERE mc_question_id = mc_question.id) @> _tag_names OR
			-- OR search (&& = Overlaps)
			NOT _tags_and AND ARRAY(SELECT name FROM mc_question_tags WHERE mc_question_id = mc_question.id) && _tag_names
		)
	)
	AND (ARRAY_LENGTH(_instructors, 1) IS NULL OR owner_id = ANY(_instructors))
	-- instructor specific filters
	AND (
		_instructor_id IS NULL OR
		-- only exercises from courses that the instructor has access to
		has_instructor_access_to_course(mc_question.course_id,_instructor_id)
	)
	-- ignore questions that already exists on the specified instructor mc worksheet (used
	-- to avoid duplicate questions when adding questions to a worksheet from the search)
	AND (
		_filter_instructor_mc_worksheet_id IS NULL OR
		NOT EXISTS (
			SELECT 1 FROM mc_worksheet_to_question wtq
			JOIN instructor_mc_worksheet imcws ON wtq.mc_worksheet_id = imcws.mc_worksheet_id
			WHERE imcws.id = _filter_instructor_mc_worksheet_id
			AND wtq.mc_question_id = mc_question.id
		)
	)
	AND is_latest_revision;

END $$ LANGUAGE plpgsql STABLE;



-- Used by JdbcAuthenticationProvider to determine the roles for a given user.
-- We don't have a table for this but simply check whether there is a student/instructor
-- table entry for the user.
-- We have to return the passed username as the first column because Spring
-- expects it this way for some stupid reason.

CREATE OR REPLACE FUNCTION get_user_roles(_username TEXT)
RETURNS TABLE(username TEXT, role TEXT)
AS $$
BEGIN
	IF EXISTS (SELECT 1 FROM v_instructor i WHERE i.username = _username) THEN
		RETURN QUERY SELECT _username,'INSTRUCTOR'::TEXT;

		-- An Instructor may also be an admin
		IF (SELECT is_admin FROM v_instructor i WHERE i.username = _username) THEN
			RETURN QUERY SELECT _username,'ADMIN'::TEXT;
		END IF;
	ELSIF EXISTS (SELECT 1 FROM v_student s WHERE s.username = _username) THEN
		RETURN QUERY SELECT _username,'STUDENT'::TEXT;
	END IF;

END $$ LANGUAGE plpgsql STABLE;
