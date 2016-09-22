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



# Expects two commits as parameters and runs all database migration scripts that
# have been added to the repository between these commits, i.e. all migration
# scripts that need to be run to migrate a database from one commit to the other.
#
# This should typically be used with git tags so you can easily migrate the database
# from one version to another. For example, if you have a database for Poodle v0.1
# and want to upgrade it to v0.2, simply run:
#
# migrate_db.sh v0.1 v0.2
#
# Or to update to the current git version:
#
# migrate_db.sh v0.1 HEAD

set -e

fromCommit=$1
toCommit=$2

if [[ -z $fromCommit || -z $toCommit ]]; then
	echo "missing parameter"
	exit 1
fi

root=$(dirname "$0")/..

migrationsPath="$root/sql/migrations/"

# Get a list of the migration scripts.
# We only care about migration scripts that were added (diff-filter)
# between the two commits.
# "basename" is used to get the filename and the "sort" sorts
# the list by the prefixed number (1_ etc.).
scripts=( $(
	git diff --diff-filter=A --name-only $fromCommit $toCommit -- "$migrationsPath" | \
	while read f; do basename "$f"; done | \
	sort -n -t_ -k1,1
) )

if [[ ${#scripts[@]} -eq 0 ]]; then
	echo 'No migration scripts between these commits, nothing to do.'
	exit 0
fi

# Write a SQL script to stdout which runs
# the migration scripts. We then pipe this to psql.
#
# We don't simply execute the files with
# psql one by one because we want all scripts
# to be run within the same transaction.
(
	# abort immediately on error
	echo '\set ON_ERROR_STOP'

	# execute the migration scripts
	for script in "${scripts[@]}"; do
		echo "\\echo executing $script..."
		echo "\\i $migrationsPath/$script"
	done
) | psql -q -U poodle -1

echo 'done.'
