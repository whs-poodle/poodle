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



DROP TRIGGER IF EXISTS exercise_set_root_id_trigger ON exercise;
DROP TRIGGER IF EXISTS mc_question_set_root_id_trigger ON mc_question;
DROP TRIGGER IF EXISTS check_mc_answer_id_trigger ON mc_chosen_answer;
DROP TRIGGER IF EXISTS check_statistic_unique_exercise_and_student_trigger ON statistic;
DROP TRIGGER IF EXISTS mc_worksheet_to_question_fix_number_trigger ON mc_worksheet_to_question;
DROP TRIGGER IF EXISTS chapter_to_exercise_fix_number_trigger ON chapter_to_exercise;



CREATE OR REPLACE FUNCTION exercise_set_root_id() RETURNS TRIGGER AS $$
BEGIN
	IF NEW.root_id IS NULL THEN
		NEW.root_id = NEW.id;
	END IF;

	RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER exercise_set_root_id_trigger
	BEFORE INSERT ON exercise
	FOR EACH ROW EXECUTE PROCEDURE exercise_set_root_id();

CREATE TRIGGER mc_question_set_root_id_trigger
	BEFORE INSERT ON mc_question
	FOR EACH ROW EXECUTE PROCEDURE exercise_set_root_id();

COMMENT ON FUNCTION exercise_set_root_id() IS 'The root_id of an exercise always references the first revision of the exercise.
	However, if we create a new exercise, this ID obviously does not exist until we actually inserted the table row. To work around
	this, the webapp sets root_id = NULL for a new exercise. This trigger then takes care of setting the root_id to generated ID.
	This trigger can be used for both "exercise" and "mc_question" since the column names are the same.';



CREATE OR REPLACE FUNCTION check_mc_answer_id() RETURNS TRIGGER AS $$
BEGIN
	IF NOT EXISTS (
		SELECT 1 FROM mc_question_to_answer ata
			WHERE ata.id = NEW.mc_answer_id
			-- get question ID by joining to the mc_statistic
			AND mc_question_id = (
				SELECT mc_question_id FROM mc_worksheet_to_question uta
					JOIN mc_statistic s ON s.id = NEW.mc_statistic_id
					WHERE uta.id = s.mc_worksheet_to_question_id
			)
	) THEN
		RAISE EXCEPTION 'mc_answer % doesnt match mc_statistic %', NEW.mc_answer_id, NEW.mc_statistic_id;
	END IF;

	RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER check_mc_answer_id_trigger
	BEFORE INSERT OR UPDATE ON mc_chosen_answer
	FOR EACH ROW EXECUTE PROCEDURE check_mc_answer_id();

COMMENT ON FUNCTION check_mc_answer_id() IS 'Makes sure that the answers inserted int mc_chosen_answer
	actually match the question that the statistic references.';



CREATE OR REPLACE FUNCTION check_statistic_unique_exercise_and_student() RETURNS TRIGGER AS $$
BEGIN
	IF EXISTS (
		SELECT FROM v_statistic
			WHERE student_id = NEW.student_id
			AND exercise_root_id = (SELECT root_id FROM exercise WHERE id = NEW.exercise_id)
			AND id != NEW.id
	) THEN
		RAISE EXCEPTION 'a statistic for student % and another revision of exercise % already exists',
			NEW.student_id, NEW.exercise_id;
	END IF;

	RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER check_statistic_unique_exercise_and_student_trigger
	BEFORE INSERT OR UPDATE ON statistic
	FOR EACH ROW EXECUTE PROCEDURE check_statistic_unique_exercise_and_student();

COMMENT ON FUNCTION check_statistic_unique_exercise_and_student() IS 'This trigger makes sure that there is only ever one entry in statistic
	for a particular student and exercise. We need this since the existing UNIQUE constraint would not avoid the same student creating a statistic for a
	different revision of the same exercise.';



CREATE OR REPLACE FUNCTION mc_worksheet_to_question_fix_number() RETURNS TRIGGER AS $$
BEGIN
	UPDATE mc_worksheet_to_question
		SET number = number - 1
		WHERE mc_worksheet_id = OLD.mc_worksheet_id
		AND number > OLD.number;

	RETURN OLD;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER mc_worksheet_to_question_fix_number_trigger
	AFTER DELETE ON mc_worksheet_to_question
	FOR EACH ROW EXECUTE PROCEDURE mc_worksheet_to_question_fix_number();

COMMENT ON FUNCTION mc_worksheet_to_question_fix_number() IS 'Makes sure that there are no gaps in the numbering of the
	questions if a question on an MC worksheet is deleted.';


CREATE OR REPLACE FUNCTION chapter_to_exercise_fix_number() RETURNS TRIGGER AS $$
BEGIN
	UPDATE chapter_to_exercise
		SET number = number - 1
		WHERE chapter_id = OLD.chapter_id
		AND number > OLD.number;

	RETURN OLD;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER chapter_to_exercise_fix_number_trigger
	AFTER DELETE ON chapter_to_exercise
	FOR EACH ROW EXECUTE PROCEDURE chapter_to_exercise_fix_number();

COMMENT ON FUNCTION chapter_to_exercise_fix_number() IS 'Makes sure that there are no gaps in the numbering of the
	exercises if an exercise on a chapter is deleted.';