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



-- Initializes a completely new database.
-- This is assumed to be executed as the postgres user,
-- e.g. with "psql -U postgres -f sql/db_init.sql" .

-- make sure we are not using the poodle database because we can't drop it otherwise
\connect postgres

-- create empty database
DROP DATABASE IF EXISTS poodle;
CREATE DATABASE poodle WITH OWNER poodle;

-- switch to database
\connect poodle

-- switch to user so the tables have the correct owner
SET ROLE poodle;

-- initialize it
\ir tables.sql
\ir views.sql
\ir triggers.sql
\ir functions_general.sql
\ir functions_instructor.sql
\ir functions_student.sql
