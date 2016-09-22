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



-- Returns whether the student has already answered the specified mc question at least once.

CREATE OR REPLACE FUNCTION has_student_answered_mc_question(_student_id INT, _mc_question_root_id INT) RETURNS BOOL AS $$
BEGIN
	RETURN EXISTS (
		SELECT 1 FROM v_mc_statistic
			WHERE student_id = _student_id
			AND completed_at IS NOT NULL
			AND mc_question_root_id = _mc_question_root_id
	);
END $$ LANGUAGE plpgsql STABLE;



-- Returns matching mc questions for a generated student mc worksheet (CreateMcWorksheetForm class).
-- This is a separate function since we also use it to calculate the number of questions
-- that would be on the worksheet without actually creating it.

CREATE OR REPLACE FUNCTION get_mc_questions_for_worksheet(
	_course_term_id INT,
	_student_id INT,
	_tags INT[],
	_max_answers INT,
	_ignore_already_answered BOOL) RETURNS SETOF v_mc_question AS $$
DECLARE
	_course_id INT;
BEGIN
	_course_id := (SELECT course_id FROM course_term WHERE id = _course_term_id);

	RETURN QUERY SELECT DISTINCT q.* FROM v_mc_question q
		-- filter by tags
		LEFT JOIN mc_question_to_tag ON q.id = mc_question_to_tag.mc_question_id
		WHERE (ARRAY_LENGTH(_tags, 1) IS NULL OR tag_id = ANY(_tags))
		-- ignore already answered
		AND (
			NOT _ignore_already_answered OR
			NOT has_student_answered_mc_question(_student_id,q.root_id)
		)
		-- also consider questions that belong to a linked course
		AND is_course_linked_with(_course_id, q.course_id)

		AND visibility != 'PRIVATE'
		AND is_latest_revision
		LIMIT _max_answers;
END $$ LANGUAGE plpgsql STABLE;



-- Erstellt ein MC-Übungsblatt anhand der angegebenen
-- Kriterien. Die Parameter entsprechen der McErstelleUebungsblattForm.
-- Creates a student mc worksheet for the specified criteria (CreateMcWorksheetForm class).

CREATE OR REPLACE FUNCTION generate_student_mc_worksheet(
	_course_term_id INT,
	_student_id INT,
	_tags INT[],
	_max_answers INT,
	_ignore_already_answered BOOL) RETURNS SETOF student_mc_worksheet AS $$
DECLARE
	_mc_worksheet_id INT;
	_student_to_worksheet_id INT;
BEGIN
	-- create worksheet itself
	INSERT INTO mc_worksheet DEFAULT VALUES
		RETURNING id INTO _mc_worksheet_id;

	INSERT INTO student_mc_worksheet(id,student_id,course_term_id)
		VALUES(_mc_worksheet_id,_student_id,_course_term_id);

	-- add questions
	INSERT INTO mc_worksheet_to_question(number,mc_worksheet_id,mc_question_id)
	(
		-- get random list of questions matching the criteria
		SELECT DISTINCT row_number() OVER () AS number,_mc_worksheet_id,id
			FROM
			(
				SELECT * FROM get_mc_questions_for_worksheet(_course_term_id,_student_id,_tags,NULL,_ignore_already_answered)
				ORDER BY RANDOM()
				LIMIT _max_answers
			) AS rand_questions
	);

	IF NOT FOUND THEN
		-- no questions, delete the worksheet and abort
		DELETE FROM mc_worksheet WHERE id = _mc_worksheet_id;
		RETURN;
	END IF;

	-- set this worksheet as the current one for the student in the course term
	UPDATE student_to_course_term
		SET student_mc_worksheet_id = _mc_worksheet_id
		WHERE student_id = _student_id
		AND course_term_id = _course_term_id;

	RETURN QUERY SELECT * FROM student_mc_worksheet WHERE id = _mc_worksheet_id;
END $$ LANGUAGE plpgsql;



-- called when a student must answer a MC question. Creates the mc_statistic
-- entry so we have a value for "seen_at" or updates "seen_at" if the an
-- entry already existed.

CREATE OR REPLACE FUNCTION prepare_mc_statistic(
	_student_id INT,
	_course_term_id INT,
	_mc_worksheet_to_question_id INT) RETURNS VOID AS $$
BEGIN
	-- let's assume there already is a row and try to update the seen_at timestamp
	UPDATE mc_statistic SET seen_at = NOW()
		WHERE student_id = _student_id
		AND course_term_id = _course_term_id
		AND mc_worksheet_to_question_id = _mc_worksheet_to_question_id;

	-- row didn't exist yet, create it
	IF NOT FOUND THEN
		INSERT INTO mc_statistic(student_id,course_term_id,mc_worksheet_to_question_id)
			VALUES(_student_id,_course_term_id,_mc_worksheet_to_question_id);
	END IF;
END $$ LANGUAGE plpgsql;



-- Returns whether the student has completed the mc worksheet, i.e. answered
-- all questions on it.

CREATE OR REPLACE FUNCTION has_student_completed_mc_worksheet(_student_id INT, _mc_worksheet_id INT)
RETURNS BOOL AS $$
BEGIN
	-- LEFT JOIN the questions on the worksheet with the mc_statistic entries
	-- and check whether every one of the is completed.
	RETURN NOT EXISTS (
		SELECT 1 from mc_worksheet_to_question m
			LEFT JOIN mc_statistic s ON m.id = s.mc_worksheet_to_question_id
			AND student_id = _student_id
			WHERE mc_worksheet_id = _mc_worksheet_id
			AND completed_at IS NULL
	);
END $$ LANGUAGE plpgsql STABLE;
