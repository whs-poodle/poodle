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
/*
 * Code for the highscore chart displayed in the
 * MC results for a student (McResultsController) and the
 * statistics for an instructor (InstructorMcWorksheetController).
 */
/* exported highscoreChart */
var highscoreChart = (function() {
	/* global messages */
	/* global utils */
	"use strict";

	// mininum number of highscore entries needed to display the chart
	var MIN_HIGHSCORE_ENTRIES = 2;

	var highscoreChartOptions = {
			title : messages.highscoreChartTitle,
			backgroundColor: "transparent",

			legend: {
				position: "none"
			},

			hAxis: {
				titleTextStyle: {
					bold: true,
					italic: false
				},
				minValue : 0,
				title: messages.points
			},

			vAxis: {
				title: messages.count,
				minValue: 4,

				titleTextStyle: {
					bold: true,
					italic: false
				},

				// calculate number of gridlines automatically
				gridlines: {
					count: -1
				}
			}
		};

	function draw(highscoreList, maxWorksheetPoints, chartId) {
		// not enough data, don't draw the chart
		if (highscoreList.length < MIN_HIGHSCORE_ENTRIES)
			return;

		// create dataTable with the points as the only column
		var dataTable = new google.visualization.DataTable();
		dataTable.addColumn("number", messages.points);

		highscoreList.forEach(function(entry) {
			dataTable.addRow([entry.points]);
		});

		var chart = new google.visualization.Histogram(document.getElementById(chartId));

		var options = $.extend(true, {}, highscoreChartOptions, {
			/* make sure the hAxis spans from 0 to the maximum number of
			 * points for this worksheet. */
			hAxis: {
				maxValue : maxWorksheetPoints
			}
		});

		chart.draw(dataTable, options);
	}

	return {
		init : function(mcWorksheetId, chartId) {
			$.ajax({
				url : utils.contextPath + "common/rest/mcWorksheetHighscore/" + mcWorksheetId,
				type : "GET",

				success : function(data) {
					var highscoreList = data.highscoreList;
					var maxWorksheetPoints = data.maxWorksheetPoints;

					draw(highscoreList, maxWorksheetPoints, chartId);
				}
			});
		}
	};
})();