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



# Drops the local database and restores
# it from the provided server.

set -e

if [[ -z $1 ]]; then
	echo 'missing server parameter'
	exit 1
fi

# Not piping directly into pg_restore here
# since this fails on Windows.

ssh "$1" pg_dump -U poodle -F c > dump
dropdb --if-exists -U postgres poodle
createdb -U postgres poodle
pg_restore -U postgres -d poodle dump
rm dump
