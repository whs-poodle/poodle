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


set -e

# disable useless NOTICE messages by psql
export PGOPTIONS='--client-min-messages=warning'

# Read the password for postgres ourselves and export it
# because the user would be asked multiple times otherwise.
read -s -p 'Password for PostgreSQL user "postgres": ' postgresPassword
echo

export PGPASSWORD="$postgresPassword"

# check whether the database already exists
if psql -U postgres -lt | cut -d \| -f 1 | grep -q -w poodle; then
	read -p 'Database "poodle" already exists. The database and the user will be dropped and recreated. Continue [y/n]? ' answer
	[[ $answer == 'y' ]] || exit 1

	echo 'Dropping existing user and database...'
	dropdb --if-exists -U postgres poodle
	dropuser --if-exists -U postgres poodle
fi

# We are not using createuser because we need the password
# so we can write it to application.properties later.
echo 'Please enter a password for the new PostgreSQL user "poodle":'
read -s -p 'Password: ' poodlePassword
echo
read -s -p 'Confirm: ' poodlePasswordConfirm
echo

if [[ $poodlePassword != $poodlePasswordConfirm ]]; then
	echo 'Passwords do not match.'
	exit 1
fi

# escape ' by replacing them with ''
poodlePasswordEscaped=${poodlePassword//\'/\'\'}

echo 'Creating new PostgreSQL user poodle...'
psql -q -U postgres -c "CREATE USER poodle WITH password '${poodlePasswordEscaped}'"

echo 'Initializing database...'
psql -q -U postgres -f sql/init_empty_db.sql

echo 'Writing password to application.properties...'
if [[ -f application.properties ]]; then
	echo 'application.properties already exists, not writing password.'
else
	echo "spring.datasource.password=${poodlePassword}" > application.properties
fi
