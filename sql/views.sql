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



CREATE OR REPLACE VIEW v_statistic AS
	SELECT statistic.*,
	(completion_status IS NULL AND difficulty IS NULL AND fun IS NULL AND time IS NULL AND statistic.text IS NULL) AS empty,
	exercise.root_id AS exercise_root_id
	FROM statistic
	JOIN exercise ON exercise.id = statistic.exercise_id;

COMMENT ON VIEW v_statistic IS ' View for statistics.
	Adds the root ID of the exercise and whether the statistic is empty.';



CREATE OR REPLACE VIEW v_exercise AS
	SELECT e.*,
		-- The window frame groups by root_id and orders by the date
		-- descending. Der first row of each row must therefore be the
		-- latest revision.
		ROW_NUMBER() OVER w = 1 AS is_latest_revision,
		LEAD(id) OVER w AS previous_revision_id, -- ID of the previous revision, NULL if none
		LAST_VALUE(changed_by_id) OVER w AS owner_id -- changed_by of the first revision = owner
		FROM exercise e
		WINDOW w AS (
			PARTITION BY e.root_id ORDER BY created_at DESC
			-- extend the window frame to the end so LAST_VALUE() works as expected
			RANGE BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
		);

COMMENT ON VIEW v_exercise IS 'View for exercises that contains some additional attributes.';



CREATE OR REPLACE VIEW v_exercise_search_result AS
	-- subquery for the total statistics of this exercise
	WITH stats AS (
		SELECT
			exercise_root_id,
			SUM(CASE WHEN NOT empty THEN 1 END) AS feedback_count, -- number of non-empty feedbacks
			COUNT(completed_at) AS completed_count, -- how often this exercise was completed
			AVG(fun) FILTER(WHERE NOT ignore) avg_fun,
			AVG(time) FILTER(WHERE NOT ignore)::INT avg_time,
			AVG(difficulty) FILTER(WHERE NOT ignore) avg_difficulty
			FROM v_statistic s
			GROUP BY exercise_root_id
	)
	SELECT e.*,
		(s.feedback_count IS NOT NULL AND s.feedback_count > 0) AS has_feedback,
		COALESCE(s.feedback_count,0) feedback_count,
		COALESCE(s.completed_count,0) completed_count,
		s.avg_fun,s.avg_time,s.avg_difficulty
		FROM v_exercise e
		LEFT JOIN stats s ON s.exercise_root_id = e.root_id
		WHERE is_latest_revision;

COMMENT ON VIEW v_exercise_search_result IS 'View for exercise search results,
	i.e. exercises plus additional attributes needed for the search (total statistics etc.).';



CREATE OR REPLACE VIEW v_mc_statistic AS
	SELECT s.*,mc_worksheet_id,mc_question_id,root_id AS mc_question_root_id
		FROM mc_statistic s
		JOIN mc_worksheet_to_question wtq ON wtq.id = s.mc_worksheet_to_question_id
		JOIN mc_question ON mc_question.id = mc_question_id;

COMMENT ON VIEW v_mc_statistic IS 'View fo mc_statistic that also returns the mc_worksheet_id and mc_question_id';



CREATE OR REPLACE VIEW v_mc_question AS
	SELECT *,
		ROW_NUMBER() OVER w = 1 AS is_latest_revision,
		LEAD(id) OVER w AS previous_revision_id,
		LAST_VALUE(changed_by_id) OVER w AS owner_id
		FROM mc_question
		WINDOW w AS (
			PARTITION BY root_id ORDER BY created_at DESC
			RANGE BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
		);

COMMENT ON VIEW v_mc_question IS 'View for mc_question. Similiar to mc_exercise (see comments there).';



CREATE OR REPLACE VIEW v_course_term AS
	SELECT *,
	-- the window groups by course_id, ordered by the course_term id descending, so the
	-- first row in the each group must be the latest course term for the course.
	row_number() OVER w = 1 AS is_latest
	FROM course_term
	WINDOW w AS (
		PARTITION BY course_id ORDER BY id DESC
	);

COMMENT ON VIEW v_course_term IS 'course_term view which adds a row which tells whether the course term is the latest for the course.';



CREATE OR REPLACE VIEW v_instructor AS
	SELECT instructor.*,poodle_user.username FROM instructor
		JOIN poodle_user ON poodle_user.id = instructor.id;

COMMENT ON VIEW v_instructor IS 'convenience views for instructors';



CREATE OR REPLACE VIEW v_student AS
	SELECT student.*,poodle_user.username FROM student
		JOIN poodle_user ON poodle_user.id = student.id;

COMMENT ON VIEW v_student IS 'convenience views for students';