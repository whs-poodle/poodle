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



-- Add an exercise to a chapter and takes take of the correct order.
-- _end defines whether the exercise should be added to the beginning
-- or the end of the chapter.

CREATE OR REPLACE FUNCTION add_exercise_to_chapter (_chapter_id INT, _exercise_id INT, _end BOOL) RETURNS VOID AS $$
DECLARE
	_number INT;
BEGIN
	IF _end THEN
		SELECT COUNT(*)+1 INTO _number FROM chapter_to_exercise
			WHERE chapter_id = _chapter_id;
	ELSE
		_number := 1;

		UPDATE chapter_to_exercise SET number = number + 1
			WHERE chapter_id = _chapter_id;
	END IF;

	INSERT INTO chapter_to_exercise(chapter_id,exercise_id,number)
		VALUES(_chapter_id,_exercise_id,_number);
END $$ LANGUAGE plpgsql;



-- Adds a chapter to a exercise worksheet while taking care of the order.

CREATE OR REPLACE FUNCTION add_chapter_to_worksheet (_worksheet_id INT, _title TEXT) RETURNS INT AS $$
DECLARE
	_number INT;
	_new_id INT;
BEGIN
	SELECT COUNT(*)+1 INTO _number FROM chapter WHERE worksheet_id = _worksheet_id;

	INSERT INTO chapter(worksheet_id,number,title) VALUES(_worksheet_id,_number,_title)
	RETURNING id INTO _new_id;

	RETURN _new_id;
END $$ LANGUAGE plpgsql;



-- Creates an exercise.

CREATE OR REPLACE FUNCTION create_exercise
(_text TEXT,
 _root_id INT, _visibility exercise_visibility,
 _title TEXT, _changed_by_id INT, hint1 TEXT, hint2 TEXT,
 _sample_solution_file_id INT, _sample_solution_text TEXT, _attachment_ids INT[], _course_id INT, _comment TEXT) RETURNS INT AS $$
DECLARE
	_new_id INT;
	a INT;
BEGIN
	-- exercise itself
	INSERT INTO exercise(text,
						root_id,visibility,title,changed_by_id,
						hint1,hint2,sample_solution_file_id,sample_solution_text,course_id,comment)
	VALUES(_text,_root_id,
			_visibility,_title,_changed_by_id,
			hint1,hint2,_sample_solution_file_id,_sample_solution_text,_course_id,_comment)
	RETURNING id INTO _new_id;

	-- attachments
	FOREACH a IN ARRAY _attachment_ids LOOP
		INSERT INTO attachment(exercise_id,file_id) VALUES(_new_id,a);
	END LOOP;

	-- if this is a new revision, update it in existing worksheets/chapters
	IF _root_id IS NOT NULL THEN
		UPDATE chapter_to_exercise SET exercise_id = _new_id
			WHERE exercise_id IN (SELECT id FROM exercise WHERE root_id = _root_id);
	END IF;

	RETURN _new_id;
END $$ LANGUAGE plpgsql;



-- Calculates the course statistics. Returns a row for each course term, with each row
-- containing the number of completed exercises (see also CourseStatistics class).

CREATE OR REPLACE FUNCTION get_course_statistics(_course_id INT)
RETURNS TABLE(
	self_study_completed INT, worksheet_completed INT, total_completed INT,
	self_study_feedbacks INT, worksheet_feedbacks INT, total_feedbacks INT,
	worksheet_students INT, self_study_students INT, total_students INT,
	enrolled_students INT,
	course_term_id INT
) AS $$
BEGIN
	RETURN QUERY SELECT
	SUM(CASE WHEN s.source = 'SELF_STUDY' THEN 1 END)::INT AS self_study_completed,
	SUM(CASE WHEN s.source = 'EXERCISE_WORKSHEET' THEN 1 END)::INT AS worksheet_completed,
	SUM(CASE WHEN s.source IS NOT NULL THEN 1 END)::INT AS total_completed, -- IS NOT NULL due to LEFT JOIN below
	SUM(CASE WHEN s.source = 'SELF_STUDY' AND NOT s.empty THEN 1 END)::INT AS self_study_feedbacks,
	SUM(CASE WHEN s.source = 'EXERCISE_WORKSHEET' AND NOT s.empty THEN 1 END)::INT AS worksheet_feedbacks,
	SUM(CASE WHEN NOT s.empty THEN 1 END)::INT AS total_feedbacks,
	COUNT(DISTINCT (CASE WHEN s.source = 'EXERCISE_WORKSHEET' THEN student_id END))::INT AS worksheet_students,
	COUNT(DISTINCT (CASE WHEN s.source = 'SELF_STUDY' THEN student_id END))::INT AS self_study_students,
	COUNT(DISTINCT (student_id))::INT AS total_students,
	(SELECT COUNT(1) FROM student_to_course_term shs WHERE shs.course_term_id = course_term.id)::INT AS enrolled_students,
	course_term.id course_term_id
	FROM course_term
	LEFT JOIN v_statistic s ON course_term.id = s.course_term_id
	WHERE course_term.course_id = _course_id
	GROUP BY course_term.id
	ORDER BY course_term.id DESC;
END $$ LANGUAGE plpgsql STABLE;



-- Returns the daily statistics for a course term.
-- Used for the diagram on the course statistics page.

CREATE OR REPLACE FUNCTION get_daily_course_term_statistics(_course_term_id INT)
RETURNS TABLE(
	day DATE,
	self_study_completed INT, worksheet_completed INT, total_completed INT,
	self_study_feedbacks INT, worksheet_feedbacks INT, total_feedbacks INT,
	worksheet_students INT, self_study_students INT, total_students INT
) AS $$
BEGIN
	RETURN QUERY
	WITH s AS (
		SELECT * FROM v_statistic
		WHERE course_term_id = _course_term_id
	)
	SELECT
	s.completed_at::DATE AS day,
	SUM(CASE WHEN s.source = 'SELF_STUDY' THEN 1 END)::INT AS self_study_completed,
	SUM(CASE WHEN s.source = 'EXERCISE_WORKSHEET' THEN 1 END)::INT AS worksheet_completed,
	COUNT(s.*)::INT AS total_completed,

	SUM(CASE WHEN s.source = 'SELF_STUDY' AND NOT s.empty THEN 1 END)::INT AS self_study_feedbacks,
	SUM(CASE WHEN s.source = 'EXERCISE_WORKSHEET' AND NOT s.empty THEN 1 END)::INT AS worksheet_feedbacks,
	SUM(CASE WHEN NOT s.empty THEN 1 END)::INT AS total_feedbacks,

	COUNT(DISTINCT (CASE WHEN s.source = 'EXERCISE_WORKSHEET' THEN student_id END))::INT AS worksheet_students,
	COUNT(DISTINCT (CASE WHEN s.source = 'SELF_STUDY' THEN student_id END))::INT AS self_study_students,
	COUNT(DISTINCT (student_id))::INT AS total_students
	FROM s
	GROUP BY day
	ORDER BY day DESC;
END $$ LANGUAGE plpgsql STABLE;



-- Moves an exercise up/down within a worksheet/chapter.

CREATE OR REPLACE FUNCTION move_exercise_in_worksheet (_chapter_id INT, _exercise_id INT, _up BOOL) RETURNS VOID AS $$
DECLARE
	_other_exercise_id INT;
	_exercise_number INT;
	_chapter_number INT;
	_worksheet_id INT;
	_other_chapter_id INT;
	_up_down_offset INT;
BEGIN
	_up_down_offset := (CASE WHEN _up THEN -1 ELSE 1 END);

	SELECT number INTO _exercise_number FROM chapter_to_exercise
		WHERE chapter_id = _chapter_id AND exercise_id = _exercise_id;

	SELECT exercise_id INTO _other_exercise_id FROM chapter_to_exercise
		WHERE chapter_id = _chapter_id AND number = _exercise_number + _up_down_offset;

	-- if this is false, the exercise is already at the top/bottom of the chapter
	IF _other_exercise_id IS NOT NULL THEN
		-- swap the numbers on the rows
		UPDATE chapter_to_exercise AS k1
		SET number = k2.number
		FROM chapter_to_exercise AS k2
		WHERE k1.chapter_id = _chapter_id AND k2.chapter_id = _chapter_id
		AND k1.exercise_id IN (_exercise_id,_other_exercise_id)
		AND k2.exercise_id IN (_exercise_id,_other_exercise_id)
		AND k1.exercise_id != k2.exercise_id;
	ELSE
		-- exercise is already at top/bottom of chapter, move it to next/previous chapter

		SELECT worksheet_id,number INTO _worksheet_id,_chapter_number
			FROM chapter WHERE id = _chapter_id;

		SELECT id INTO _other_chapter_id FROM chapter
			WHERE worksheet_id = _worksheet_id
			AND number = _chapter_number + _up_down_offset;

		-- if this is false, there is no next/previous chapter
		IF _other_chapter_id IS NOT NULL THEN
			PERFORM add_exercise_to_chapter(_other_chapter_id, _exercise_id, _up);
			DELETE FROM chapter_to_exercise
				WHERE chapter_id = _chapter_id AND exercise_id = _exercise_id;
		END IF;
	END IF;
END $$ LANGUAGE plpgsql;



-- Moves a chapter within an exercise worksheet.

CREATE OR REPLACE FUNCTION move_chapter (_chapter_id INT, _up BOOL) RETURNS VOID AS $$
DECLARE
	_other_chapter_id INT;
	_number INT;
	_worksheet_id INT;
	_up_down_offset INT;
BEGIN
	_up_down_offset := (CASE WHEN _up THEN -1 ELSE 1 END);

	SELECT worksheet_id,number INTO _worksheet_id,_number
		FROM chapter WHERE id = _chapter_id;

	SELECT id INTO _other_chapter_id FROM chapter
		WHERE worksheet_id = _worksheet_id
		AND number = _number + _up_down_offset;

	-- if this is false, the chapter is already at the bottom/top
	IF _other_chapter_id IS NOT NULL THEN
		-- swap the numbers
		UPDATE chapter AS k1
		SET number = k2.number
		FROM chapter AS k2
		WHERE k1.id IN (_chapter_id,_other_chapter_id)
		AND k2.id IN (_chapter_id,_other_chapter_id)
		AND k1.id != k2.id;
	END IF;
END $$ LANGUAGE plpgsql;



-- Removes a chapter from an exercise worksheet and takes care of the order.

CREATE OR REPLACE FUNCTION remove_chapter (_chapter_id INT) RETURNS VOID AS $$
DECLARE
	_worksheet_id INT;
	_number INT;
BEGIN
	SELECT worksheet_id,number INTO _worksheet_id,_number
	FROM chapter WHERE id = _chapter_id;

	UPDATE chapter SET number = number - 1
	WHERE worksheet_id = _worksheet_id AND number > _number;

	DELETE FROM chapter WHERE id = _chapter_id;
END $$ LANGUAGE plpgsql;



-- Creates a course and the first term.

CREATE OR REPLACE FUNCTION create_course
(_instructor_id INT, _name TEXT, _visible BOOL, _password TEXT, _instructor_ids INT[], _linked_course_ids INT[], _first_term TEXT)
RETURNS INT AS $$
DECLARE
	_new_course_id INT;
	d INT;
	m INT;
BEGIN
	-- course itself
	INSERT INTO course(instructor_id,name,visible,password)
		VALUES(_instructor_id,_name,_visible,_password)
		RETURNING id INTO _new_course_id;

	-- other instructors
	FOREACH d IN ARRAY _instructor_ids LOOP
		INSERT INTO course_to_instructor(course_id,instructor_id)
			VALUES(_new_course_id,d);
	END LOOP;

	-- linked courses
	FOREACH m IN ARRAY _linked_course_ids LOOP
		INSERT INTO course_to_linked_course(course_id,linked_course_id)
			VALUES(_new_course_id,m);
	END LOOP;

	-- term
	INSERT INTO course_term(course_id,term)
		VALUES(_new_course_id,_first_term);

	RETURN _new_course_id;
END $$ LANGUAGE plpgsql;


-- Updates an existing course.

CREATE OR REPLACE FUNCTION update_course
(_course_id INT, _name TEXT, _visible BOOL, _password TEXT, _instructor_ids INT[], _linked_course_ids INT[])
RETURNS VOID AS $$
DECLARE
	d INT;
	m INT;
BEGIN
	UPDATE course SET name = _name, visible = _visible, password = _password WHERE id = _course_id;

	-- delete old entries before recreating the new ones
	DELETE FROM course_to_instructor WHERE course_id = _course_id;
	DELETE FROM course_to_linked_course WHERE course_id = _course_id;

	FOREACH d IN ARRAY _instructor_ids LOOP
		INSERT INTO course_to_instructor(course_id,instructor_id)
			VALUES(_course_id,d);
	END LOOP;

	FOREACH m IN ARRAY _linked_course_ids LOOP
		INSERT INTO course_to_linked_course(course_id,linked_course_id)
			VALUES(_course_id,m);
	END LOOP;
END $$ LANGUAGE plpgsql;



-- Creates a fake student for the specified instructor.

CREATE OR REPLACE FUNCTION create_fake_student(_instructor_id INT) RETURNS INT AS $$
DECLARE
	_username TEXT;
	_user_id INT;
	_student_id INT;
BEGIN
	-- Check whether a fake student already exists.
	SELECT id FROM student INTO _student_id WHERE fake_for_instructor_id = _instructor_id;

	IF _student_id IS NULL THEN
		-- No fake student yet, create one. We use the instructors'
		-- username with a suffix as the username for the fake student.
		_username := (SELECT username FROM v_instructor WHERE id = _instructor_id) || '_studentmode';

		INSERT INTO poodle_user(username) VALUES(_username)
			RETURNING id INTO _user_id;
		INSERT INTO student(id,fake_for_instructor_id) VALUES(_user_id,_instructor_id)
			RETURNING id INTO _student_id;

		-- enroll fake student into all course terms of this instructor
		INSERT INTO student_to_course_term(student_id,course_term_id)
			SELECT _student_id,v_course_term.id FROM v_course_term
				JOIN course ON course.id = v_course_term.course_id
				WHERE course.instructor_id = _instructor_id
				AND v_course_term.is_latest;
	END IF;

	RETURN _student_id;
END $$ LANGUAGE plpgsql;



-- Returns whether instructor has access to the course (owner or via linked course)

CREATE OR REPLACE FUNCTION has_instructor_access_to_course(_course_id INT, _instructor_id INT) RETURNS BOOL AS $$
BEGIN
	RETURN
		(SELECT instructor_id FROM course WHERE id = _course_id) = _instructor_id OR -- owner?
		EXISTS (SELECT 1 FROM course_to_instructor WHERE course_id = _course_id AND instructor_id = _instructor_id);
END $$ LANGUAGE plpgsql STABLE;



-- Merges multiple tags into a single one. The specified tags should all be part of the same course.
-- _merge_to is the ID of the "new" tag that the other tags are being merged to (_merge_to is also in _tag_ids).

CREATE OR REPLACE FUNCTION merge_tags(_tag_ids INT[], _merge_to INT) RETURNS VOID AS $$
DECLARE
	t INT;
BEGIN
	-- Loop through all tags that are merged into the "new" tag and can therefore be removed
	FOREACH t IN ARRAY _tag_ids LOOP
		IF _merge_to != t THEN
			-- remove the tag on all exercises that already have a relation to the "new" tag
			DELETE FROM exercise_to_tag att
				WHERE att.tag_id = t
				AND EXISTS(
					SELECT 1 FROM exercise_to_tag att2
						WHERE att2.exercise_id = att.exercise_id AND att2.tag_id = _merge_to
				);

			-- update the rest to the new tag
			UPDATE exercise_to_tag SET tag_id = _merge_to WHERE tag_id = t;

			-- same thing for mc questions...
			DELETE FROM mc_question_to_tag matt
				WHERE matt.tag_id = t
				AND EXISTS(
					SELECT 1 FROM mc_question_to_tag matt2
						WHERE matt2.mc_question_id = matt.mc_question_id AND matt2.tag_id = _merge_to
				);

			UPDATE mc_question_to_tag SET tag_id = _merge_to WHERE tag_id = t;

			-- delete the tag
			DELETE FROM tag WHERE id = t;
		END IF;
	END LOOP;
END $$ LANGUAGE plpgsql;
