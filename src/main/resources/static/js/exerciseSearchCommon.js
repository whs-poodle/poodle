/*
 * Copyright 2015 Westfälische Hochschule
 *
 * This file is part of Poodle.
 *
 * Poodle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Poodle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Poodle.  If not, see <http://www.gnu.org/licenses/>.
 */
/* exported exerciseSearch */

/*
 *Code used by exerciseSearch.js and mcQuestionSearch.js
 */
var exerciseSearch = (function() {
	"use strict";

	return {
		// table is the object created by DataTables in initResultsTable
		removeRow: function(table, exerciseId) {
			var $row = $("tr[data-exercise-id=" + exerciseId + "]");
			table.row($row).remove().draw(false);
		},

		// create the DataTable and return it
		initResultsTable : function() {
			return $("#resultsTable").DataTable({
				aaSorting: [], // don't sort initially
				iDisplayLength: 25,
				bFilter : true
			});
		}
	};
})();