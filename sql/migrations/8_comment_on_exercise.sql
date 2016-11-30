ALTER TABLE exercise ADD COLUMN comment TEXT;
ALTER TABLE mc_question ADD COLUMN comment TEXT;
DROP VIEW v_exercise CASCADE;
DROP VIEW v_mc_question CASCADE;
\ir ../views.sql
\ir ../functions_general.sql
\ir ../functions_instructor.sql
\ir ../functions_student.sql