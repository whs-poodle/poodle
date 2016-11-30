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



-- enums must be in sync with Java enums

CREATE TYPE completion_status
	AS ENUM('COMPLETELY', 'PARTLY', 'NOTATALL');

CREATE TYPE exercise_visibility
	AS ENUM('PUBLIC', 'PRIVATE');



CREATE TABLE poodle_user (
	id SERIAL PRIMARY KEY,
	username TEXT UNIQUE NOT NULL CHECK(username = LOWER(username)),
	password_hash CHAR(60) UNIQUE
);

COMMENT ON TABLE poodle_user IS 'User super table for instructor and student';
COMMENT ON COLUMN poodle_user.username IS 'Usernames must always be lower case in the database.';
COMMENT ON COLUMN poodle_user.password_hash IS 'bcrypt hash';



CREATE TABLE instructor (
	id INT PRIMARY KEY REFERENCES poodle_user(id) ON DELETE CASCADE,
	last_name TEXT NOT NULL,
	first_name TEXT NOT NULL,
	last_login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	is_admin BOOL NOT NULL DEFAULT FALSE
);



CREATE TABLE course (
	id SERIAL PRIMARY KEY,
	instructor_id INT NOT NULL REFERENCES instructor(id),
	name TEXT NOT NULL,
	visible BOOL NOT NULL DEFAULT TRUE,
	password TEXT,
	UNIQUE(instructor_id,name)
);

COMMENT ON TABLE course IS 'Course.
	A course always belongs to a single instructor.
	However, other instructors can be given access via course_to_instructor.

	instructor_id and name are UNIQUE since multiple courses with the same name can exist,
	but not	for one instructor.';



CREATE TABLE course_to_instructor (
	course_id INT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
	instructor_id INT NOT NULL REFERENCES instructor(id),
	PRIMARY KEY(course_id,instructor_id)
);

COMMENT ON TABLE course_to_instructor IS 'List of other instructors that have access to this course';



CREATE TABLE course_to_linked_course (
	course_id INT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
	linked_course_id INT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
	PRIMARY KEY(course_id,linked_course_id)
);

COMMENT ON TABLE course_to_linked_course IS 'List of linked courses.
	If course A is linked with course B, all students enrolled in course B can also access exercises and questions from courses A.';



CREATE TABLE student (
	id INT PRIMARY KEY REFERENCES poodle_user(id) ON DELETE CASCADE,
	fake_for_instructor_id INT REFERENCES instructor(id),
	cfg_email_messages BOOL DEFAULT TRUE,
	cfg_email_worksheet_unlocked BOOL DEFAULT FALSE
);



CREATE TABLE uploaded_image (
	id SERIAL PRIMARY KEY,
	instructor_id INT NOT NULL REFERENCES instructor(id),
	mimetype TEXT NOT NULL,
	data BYTEA NOT NULL,
	filename TEXT NOT NULL,
	uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	course_id INT NOT NULL REFERENCES course(id) ON DELETE CASCADE
);

COMMENT ON TABLE uploaded_image IS 'Images upladed by instructors via CKEditor.';



CREATE TABLE course_term (
	id SERIAL PRIMARY KEY,
	course_id INT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
	term TEXT NOT NULL,
	UNIQUE(course_id,term)
);

COMMENT ON TABLE course_term IS 'A course term defines a term in a specific course.';



CREATE TABLE mc_question (
	id SERIAL PRIMARY KEY,
	course_id INT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
	changed_by_id INT NOT NULL REFERENCES instructor(id),
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	root_id INT NOT NULL REFERENCES mc_question(id) ON DELETE CASCADE,
	text TEXT NOT NULL,
	visibility exercise_visibility NOT NULL,
	has_multiple_correct_answers BOOL NOT NULL,
	comment TEXT
);

COMMENT ON TABLE mc_question IS 'Multiple Choice questions';



CREATE TABLE mc_question_to_answer (
	id SERIAL PRIMARY KEY,
	mc_question_id INT NOT NULL REFERENCES mc_question(id) ON DELETE CASCADE,
	correct BOOL NOT NULL,
	text TEXT NOT NULL CHECK(text != '')
);

COMMENT ON TABLE mc_question_to_answer IS 'Answers for a MC question';



CREATE TABLE mc_worksheet (
	id SERIAL PRIMARY KEY
);

COMMENT ON TABLE mc_worksheet IS 'Super class for MC worksheets.
	We have two separate types of MC worksheets, those created by students and those created by instructors.
	We need this super table so we can use mc_worksheet_to_question with both types.';



CREATE TABLE student_mc_worksheet (
	id INT PRIMARY KEY REFERENCES mc_worksheet(id) ON DELETE CASCADE,
	course_term_id INT NOT NULL REFERENCES course_term(id) ON DELETE CASCADE,
	student_id INT NOT NULL REFERENCES student(id) ON DELETE CASCADE,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	is_public BOOL NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE student_mc_worksheet IS 'MC worksheets created by students';
COMMENT ON COLUMN student_mc_worksheet.student_id IS 'creator';



CREATE TABLE mc_worksheet_to_question (
	id SERIAL PRIMARY KEY,
	mc_worksheet_id INT NOT NULL REFERENCES mc_worksheet(id) ON DELETE CASCADE,
	mc_question_id INT NOT NULL REFERENCES mc_question(id) ON DELETE CASCADE,
	number INT NOT NULL CHECK(number >= 1),
	UNIQUE(mc_worksheet_id,mc_question_id)
);

COMMENT ON TABLE mc_worksheet_to_question IS 'Questions on a MC worksheet.';



CREATE TABLE student_to_course_term (
	course_term_id INT NOT NULL REFERENCES course_term(id) ON DELETE CASCADE,
	student_id INT NOT NULL REFERENCES student(id) ON DELETE CASCADE,
	student_mc_worksheet_id INT REFERENCES student_mc_worksheet(id),
	id SERIAL PRIMARY KEY,
	UNIQUE(student_id,course_term_id)
);

COMMENT ON TABLE student_to_course_term IS 'Defines which course terms a student is enrolled in.';
COMMENT ON COLUMN student_to_course_term.student_mc_worksheet_id IS 'A student can only ever work on one generated MC worksheet in a course term at a time.
	This ID always references this worksheet. We detect the current question automatically from the statistics.
	This value is only NULL if the student has not created any MC worksheet yet or he canceled it. Note that
	this has nothing to do with the MC worksheets created by instructors (instructor_mc_worksheet).';



CREATE TABLE tag (
	id SERIAL PRIMARY KEY,
	name TEXT NOT NULL,
	course_id INT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
	instructor_only BOOLEAN NOT NULL,
	UNIQUE(name,course_id)
);

COMMENT ON TABLE tag IS 'Tags
	A tag always belongs to one course, but a tag with the same name can exist
	multiple times independent of each other in different courses.';



CREATE TABLE mc_question_to_tag (
	mc_question_id INT NOT NULL REFERENCES mc_question(id) ON DELETE CASCADE,
	tag_id INT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
	PRIMARY KEY (mc_question_id,tag_id)
);

COMMENT ON TABLE mc_question_to_tag IS 'Tags on a MC question.';



CREATE TABLE uploaded_file (
	id SERIAL PRIMARY KEY,
	data BYTEA NOT NULL,
	mimetype TEXT NOT NULL,
	filename TEXT NOT NULL
);

COMMENT ON TABLE uploaded_file IS 'Files (sample solutions, attachments on exercises etc.)';



CREATE TABLE exercise (
	id SERIAL PRIMARY KEY,
	course_id INT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
	changed_by_id INT NOT NULL REFERENCES instructor(id),
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	text TEXT NOT NULL,

	root_id INT NOT NULL REFERENCES exercise(id) ON DELETE CASCADE,
	title TEXT NOT NULL,
	hint1 TEXT,
	hint2 TEXT,
	visibility exercise_visibility NOT NULL,
	sample_solution_file_id INT REFERENCES uploaded_file(id),
	sample_solution_text TEXT,
	CONSTRAINT sample_solution_constraint
		CHECK (sample_solution_file_id IS NULL OR sample_solution_text IS NULL),
	comment TEXT
);

COMMENT ON TABLE exercise IS 'Exercises';
COMMENT ON COLUMN exercise.changed_by_id IS 'who created this revision';
COMMENT ON COLUMN exercise.root_id IS 'root_id always references the first revision of an exercise so we
	can easily group and identify revisions of an exercise.';
COMMENT ON CONSTRAINT sample_solution_constraint ON exercise IS 'the sample solution is a text or a file, never both';



CREATE TABLE attachment (
	exercise_id INT NOT NULL REFERENCES exercise(id) ON DELETE CASCADE,
	file_id INT NOT NULL REFERENCES uploaded_file(id),
	PRIMARY KEY(exercise_id,file_id)
);

COMMENT ON TABLE attachment IS 'Attachments on exercises';



CREATE TABLE exercise_to_tag (
	exercise_id INT NOT NULL REFERENCES exercise(id) ON DELETE CASCADE,
	tag_id INT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
	PRIMARY KEY (exercise_id,tag_id)
);

COMMENT ON TABLE exercise_to_tag IS 'Tags on exercises.';



CREATE TABLE self_study_worksheet_to_exercise (
	exercise_id INT NOT NULL REFERENCES exercise(id) ON DELETE CASCADE,
	student_to_course_term_id INT NOT NULL REFERENCES student_to_course_term(id) ON DELETE CASCADE,
	PRIMARY KEY (student_to_course_term_id,exercise_id)
);

COMMENT ON TABLE self_study_worksheet_to_exercise IS 'Exercises on a self study worksheet.
	This references student_to_course_term since there is only one self study worksheet per student and course term.';



CREATE TYPE worksheet_type
	AS ENUM('EXERCISE', 'MC', 'EVALUATION');

CREATE TABLE worksheet (
	id SERIAL PRIMARY KEY,
	course_term_id INT NOT NULL REFERENCES course_term(id) ON DELETE CASCADE,
	unlocked BOOL NOT NULL DEFAULT FALSE,
	number INT NOT NULL CHECK(number >= 1),
	title TEXT NOT NULL,
	unlock_at TIMESTAMP,
	type worksheet_type NOT NULL
);

COMMENT ON TABLE worksheet IS 'Worksheets.
	This is the super table for exercise_worksheet and instructor_mc_worksheet.';



CREATE TABLE exercise_worksheet (
	id INT PRIMARY KEY REFERENCES worksheet(id) ON DELETE CASCADE
);

COMMENT ON TABLE exercise_worksheet IS 'Exercise Worksheets.
	This is its own table so we can make sure that chapter.worksheet_id does not reference a MC worksheet.';



CREATE TABLE instructor_mc_worksheet (
	id INT PRIMARY KEY REFERENCES worksheet(id) ON DELETE CASCADE,
	mc_worksheet_id INT NOT NULL REFERENCES mc_worksheet(id) ON DELETE CASCADE
);

COMMENT ON TABLE instructor_mc_worksheet IS 'MC worksheets created by instructors.
	We need the reference to "worksheet" for its attributes and the reference to mc_worksheet so we can add
	questions to this worksheet via mc_worksheet_to_question.';



CREATE TABLE evaluation_worksheet (
	id INT PRIMARY KEY REFERENCES worksheet(id) ON DELETE CASCADE,
	unlocked_until TIMESTAMP
);

COMMENT ON TABLE evaluation_worksheet IS 'Evaluation worksheets.
	There is only ever one evaluation worksheet per course term.';



CREATE TABLE evaluation_section (
	id SERIAL PRIMARY KEY,
	evaluation_worksheet_id INT NOT NULL REFERENCES evaluation_worksheet(id) ON DELETE CASCADE,
	number INT NOT NULL CHECK(number >= 1),
	title TEXT NOT NULL
);

COMMENT ON TABLE evaluation_section IS 'sections in an evaluation';



CREATE TABLE evaluation_question (
	id SERIAL PRIMARY KEY,
	evaluation_section_id INT NOT NULL REFERENCES evaluation_section(id) ON DELETE CASCADE,
	number INT NOT NULL CHECK(number >= 1),
	allow_not_applicable BOOL NOT NULL DEFAULT FALSE,
	text TEXT NOT NULL
);

COMMENT ON TABLE evaluation_question IS 'An evaluation question.
	One question always belongs to one specific evaluation/evaluation section, so it can not be used multiple times on
	different evaluation worksheets and there are not multiple revisions like for exercises and MC questions (this would
	not make much sense for evaluations).
	A question has either choices or the student has to provide text (in which case the question simply has no choices
	and allow_not_applicable is ignored).';
COMMENT ON COLUMN evaluation_question.allow_not_applicable IS ' whether the student can choose "n/a" instead of one of the choices';



CREATE TABLE evaluation_question_to_choice (
	id SERIAL PRIMARY KEY,
	evaluation_question_id INT NOT NULL REFERENCES evaluation_question(id) ON DELETE CASCADE,
	value INT,
	text TEXT,
	CONSTRAINT value_or_text
		CHECK(value IS NOT NULL OR text IS NOT NULL)
);

COMMENT ON TABLE evaluation_question_to_choice IS 'Choices for an evaluation question.
	The value is optional and may be used to calculate the average value for the question in the statistics, if this
	makes sense for the choices, e.g. if the choices are grades. The text is also optional since you may not want to
	specify a text for each choice if there are values (e.g. you may only want to specify "very good" for the first
	choice and "very bad" for the last choice, but not for all choices inbetween).

	Note that it is assumed that either all choices for a question have a value or none of them have. Also, if there
	are no values, each choice must have a text.';
COMMENT ON CONSTRAINT value_or_text ON evaluation_question_to_choice IS 'either text or value has to be set';



CREATE TABLE evaluation_statistic (
	id SERIAL PRIMARY KEY,
	student_id INT NOT NULL REFERENCES student(id),
	evaluation_question_id INT NOT NULL REFERENCES evaluation_question(id) ON DELETE CASCADE,
	choice_id INT REFERENCES evaluation_question_to_choice(id),
	text TEXT CHECK(text != ''),
	UNIQUE(student_id,evaluation_question_id)
);

COMMENT ON TABLE evaluation_statistic IS 'Evaluation statistics.
	Each student can only answer each evaluation question once. The choice_id is the chosen choice. If the question	has
	allow_not_applicable = TRUE and the student chose "not applicable" or if it is a text question, choice_id is NULL.';
COMMENT ON COLUMN evaluation_statistic.text IS 'only set if the question is a text question (i.e. has no choices)';


CREATE TABLE chapter (
	id SERIAL PRIMARY KEY,
	worksheet_id INT NOT NULL REFERENCES exercise_worksheet(id) ON DELETE CASCADE,
	number INT NOT NULL CHECK(number >= 1),
	title TEXT NOT NULL
);

COMMENT ON TABLE chapter IS 'Chapters on an exercise worksheet.';



CREATE TABLE chapter_to_exercise (
	exercise_id INT NOT NULL REFERENCES exercise(id) ON DELETE CASCADE,
	chapter_id INT NOT NULL REFERENCES chapter(id) ON DELETE CASCADE,
	number INT NOT NULL CHECK(number >= 1),
	id SERIAL PRIMARY KEY,
	UNIQUE(exercise_id,chapter_id)
);

COMMENT ON TABLE chapter_to_exercise IS 'Exercises in a chapter.';



CREATE TYPE statistic_source
	AS ENUM('EXERCISE_WORKSHEET', 'SELF_STUDY');

COMMENT ON TYPE statistic_source IS 'where the student has given the feedback for an exercise (see statistic table below)';



CREATE TABLE statistic (
	difficulty INT CHECK (difficulty BETWEEN 1 AND 10),
	fun INT CHECK (fun BETWEEN 1 AND 10),
	text TEXT CHECK(text != ''),
	time INT CHECK (time > 0),
	completion_status completion_status,
	completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	course_term_id INT NOT NULL REFERENCES course_term(id) ON DELETE CASCADE,
	exercise_id INT NOT NULL REFERENCES exercise(id) ON DELETE CASCADE,
	student_id INT NOT NULL REFERENCES student(id) ON DELETE CASCADE,
	id SERIAL PRIMARY KEY,
	ignore BOOL NOT NULL DEFAULT FALSE,
	source statistic_source NOT NULL,
	UNIQUE(student_id,exercise_id)
);

COMMENT ON TABLE statistic IS 'Statistics for exercises.';
COMMENT ON COLUMN statistic.completed_at IS 'when this exercise was marked as completed';
COMMENT ON COLUMN statistic.source IS 'see check_statistic_unique_exercise_and_student() trigger on why this UNIQUE constraint is not enough.';



CREATE TABLE statistic_instructor_comment (
	statistic_id INT PRIMARY KEY REFERENCES statistic(id) ON DELETE CASCADE,
	instructor_id INT REFERENCES instructor(id),
	text TEXT,
	seen BOOL NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE statistic_instructor_comment IS 'Comments given by an instructor on a feedback (one statistic has only one comment)';
COMMENT ON COLUMN statistic_instructor_comment.seen IS 'whether the comment has been seen by the student';



CREATE TABLE mc_statistic (
	id SERIAL PRIMARY KEY,
	student_id INT NOT NULL REFERENCES student(id) ON DELETE CASCADE,
	course_term_id INT NOT NULL REFERENCES course_term(id) ON DELETE CASCADE,
	mc_worksheet_to_question_id INT NOT NULL REFERENCES mc_worksheet_to_question(id) ON DELETE CASCADE,
	seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	completed_at TIMESTAMP
);

COMMENT ON TABLE mc_statistic IS 'Statistics for MC questions.
	Note that a MC statistic does not reference a MC question, but a MC question on a worksheet. This is necessary so that a student
	can answer the same question multiple times on different worksheets.';
COMMENT ON COLUMN mc_statistic.seen_at IS 'when this question was displayed to the student';
COMMENT ON COLUMN mc_statistic.completed_at IS 'when the student answered the question (NULL if not yet)';



CREATE TABLE mc_chosen_answer (
	mc_statistic_id INT NOT NULL REFERENCES mc_statistic(id) ON DELETE CASCADE,
	mc_answer_id INT NOT NULL REFERENCES mc_question_to_answer(id) ON DELETE CASCADE,
	PRIMARY KEY(mc_statistic_id,mc_answer_id)
);

COMMENT ON TABLE mc_chosen_answer IS 'Answers that a student chose for a worksheet on a question.';



CREATE TABLE lecture_note (
	id SERIAL PRIMARY KEY,
	title TEXT NOT NULL,
	groupname TEXT NOT NULL,
	num INT NOT NULL,
	course_id INT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
	file_id INT NOT NULL REFERENCES uploaded_file(id) ON DELETE CASCADE,
	UNIQUE(id, file_id)
);

COMMENT ON TABLE lecture_note IS 'Lecture notes (Fileupload for Course)';
COMMENT ON COLUMN lecture_note.num IS 'Group number for sorting';