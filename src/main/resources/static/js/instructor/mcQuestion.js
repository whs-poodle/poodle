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
$(document).ready(function() {
	/* global exercise */
	"use strict";

	// confirm deletion
	$("#deleteForm").submit(function() {
		return exercise.confirmDelete();
	});

	// initialize DataTables for statistics and calculate average
	var table = $("#statistics").DataTable({
		"order" : [[ 0, "desc" ]], // sort date descending
		"initComplete" : function() {
			var sum, count = 0;
			table.column(1).every(function() {
				// Callback function for each item that returns the next
				// accumulator and the final call returns the result.
				sum = this.data().reduce(function(acc, element) {
					count++;
					if (element.indexOf('green') > -1) {
						acc++;
					}
					return acc;
				}, 0);
				$(this.footer()).html(parseInt(sum / count * 100) + '%');
			});
			table.column(2).every(function() {
				sum = this.data().reduce(function(acc, element) {
					return acc + parseInt(element); // parses string to integer
				}, 0);
				$(this.footer()).html(parseInt(sum / count) + 's');
			});
		}
	});
});