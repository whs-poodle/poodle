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
 * For instructors/courseStatistics.html
 */
$(document).ready(function() {
	/* global messages */
	"use strict";

	// set moment.js locale
	moment.locale(messages.momentJsLocale);

	var tooltipTemplate = $("#statisticsTooltipTemplate").html();
	Mustache.parse(tooltipTemplate);

	/* draw the chart based on the stats by the server.
	 * "stats" is a map, mapping each date to its statistics.
	 *
	 * Google Charts Dashboard docs:
	 * https://developers.google.com/chart/interactive/docs/gallery/controls
	 */
	function drawChart(stats) {
		var dashboard = new google.visualization.Dashboard(document.getElementById('latestStatsDashboard'));

		var chartWrapper = new google.visualization.ChartWrapper({
			chartType: 'LineChart',
			containerId: 'latestStatsChart',
			options : {
				backgroundColor: "transparent",
				legend: { position: "top" },
				tooltip: { isHtml: true },
				height: 300
			}
		});

		var rangeFilter = new google.visualization.ControlWrapper({
			controlType: 'ChartRangeFilter',
			containerId: 'latestStatsRangeFilter',
			options: {
				filterColumnIndex: 0,
				ui : {
					chartOptions : {
						backgroundColor: "transparent",
						height : 50
					}
				},
			}
		});

		dashboard.bind(rangeFilter, chartWrapper);

		var dataTable = new google.visualization.DataTable();

		/* Columns. Every tooltip column is the tooltip
		 * for the previously defined column. */
		var tooltipColumn = {
			type : "string",
			role : "tooltip",
			p : {
				html: true
			}
		};
		dataTable.addColumn("date", messages.day);
		dataTable.addColumn("number", messages.worksheets);
		dataTable.addColumn(tooltipColumn);
		dataTable.addColumn("number", messages.selfStudy);
		dataTable.addColumn(tooltipColumn);
		dataTable.addColumn("number", messages.total);
		dataTable.addColumn(tooltipColumn);
		dataTable.addColumn("number", messages.feedbackTotal);
		dataTable.addColumn(tooltipColumn);

		for (var day in stats) {
			// the server sends us the date in ISO format (2014-10-20)
			var dayAsDate = moment(day, "YYYY-MM-DD").toDate();
			var dayStr = moment(dayAsDate).format(messages.statsJsDayFormat);

			var dailyStat = stats[day];

			// generate tooltip
			var tooltip = Mustache.render(tooltipTemplate, {
				dayStr: dayStr,
				dailyStat : dailyStat
			});

			// data for Google API
			var dailyData = [
				dayAsDate,
				dailyStat.exerciseWorksheetStats.completed, tooltip,
				dailyStat.selfStudyWorksheetStats.completed, tooltip,
				dailyStat.total.completed, tooltip,
				dailyStat.total.feedback, tooltip
			];

			dataTable.addRow(dailyData);
		}

		// set the default range for the RangeFilter
		var rangeEnd = dataTable.getColumnRange(0).max; // max Date (= latest statistic)
		var rangeStart = moment(rangeEnd).subtract(1, 'weeks').toDate(); // start = one week prior to latest statistic

		rangeFilter.setState({
			range : {
				start : rangeStart,
				end : rangeEnd,
			}
		});

		dashboard.draw(dataTable);
	}

	var $courseTermSelect = $("#courseTermSelect");

	function updateStats() {
		var courseTermId = $courseTermSelect.val();

		if (courseTermId.includes("courseId")) {
			var courseId = courseTermId.replace("courseId", '');
			courseTermId = "0";

			$.ajax({
				type : "GET",
				url : window.location.pathname+	 "/courseStatistics",
				data: {
					courseId : courseId
				},
				success : drawChart
			});
		} else {

			$.ajax({
				type : "GET",
				url : window.location.pathname+	 "/dailyStatistics",
				data: {
					courseTermId : courseTermId
				},
				success : drawChart
			});
		}

		// show the correct totalStats (enrolled students and the table)
		$(".totalStats[data-course-term-id!=" + courseTermId + "]").hide();
		$(".totalStats[data-course-term-id=" + courseTermId + "]").show();
	}

	$courseTermSelect.change(updateStats);

	// generate diagram
	google.load(
		"visualization", "1",
		{
			callback : updateStats,
			packages : ["controls"],
			language : messages.googleChartsLocale
		}
	);
});