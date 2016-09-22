#!/bin/bash

# Copyright 2015 Westfälische Hochschule
#
# This file is part of Poodle.
#
# Poodle is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Poodle is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with Poodle.  If not, see <http://www.gnu.org/licenses/>.



# Creates a new, empty database. Useful to compare the local schema
# to the schema on a server with compare_schemas.sql afterwards.
# In contrast to setup_db.sh, this script does not setup the user etc.

root=$(dirname "$0")/..
psql -U postgres -q -f "$root/sql/init_empty_db.sql"
