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
	/* global messages */
	/* global highscoreChart */
	"use strict";

	var CORRECT_ANSWER_COLOR = "green";
	var WRONG_ANSWER_COLOR = "red";

	var chartOptions = {
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
			title : messages.count
		},
		vAxis: {
			titleTextStyle: {
				bold: true,
				italic: false
			},
			title: messages.answer
		}
	};

	function loadCharts() {
		// load the highscore chart (the highscoreChart element doesn't exist if the worksheet is still locked)
		if ($("#highscoreChart").length !== 0) {
			var mcWorksheetId = $("head").data("mc-worksheet-id");
			highscoreChart.init(mcWorksheetId, "highscoreChart");
		}

		/* load the answer stats for each of the questions.
		 * Note that there are no ".answerChart" elements if the worksheet
		 * is not unlocked yet, so this is a NOOP in this case. */
		$(".answerChart").each(function() {
			var $chartDiv = $(this);
			var mcWorksheetToQuestionId = $chartDiv.data("mc-worksheet-to-question-id");

			$.ajax({
				type : "GET",
				url : "answerStats/" + mcWorksheetToQuestionId,

				success : function(answerStats) {
					/* answerStats is a list of objects with each object
					 * containing the answer and the number of times it has been
					 * chosen (count). Initialize the chart with this data. */
					initChart(mcWorksheetToQuestionId, answerStats, $chartDiv);
				}
			});
		});
	}

	// initializes a chart for a specific question
	function initChart(mcWorksheetToQuestionId, answerStats, $chartDiv) {
		var dataTable = new google.visualization.DataTable();

		dataTable.addColumn("string", messages.answer);
		dataTable.addColumn("number", messages.count);
		dataTable.addColumn({type : "string", role : "style"}); // this column defines the color of the bar

		// count of the answer that has been chosen the most
		var maxCount = 0;

		// iterate the answers with its stats
		answerStats.forEach(function(entry) {
			var color = entry.answer.correct ? CORRECT_ANSWER_COLOR : WRONG_ANSWER_COLOR;

			dataTable.addRow([entry.answer.text, entry.count, color]);

			if (entry.count > maxCount)
				maxCount = entry.count;
		});

		// this question hasn't been answered yet, don't show a chart
		if (maxCount === 0)
			return;

		// hide the answer list (it would be redundant since all the information is in the chart)
		$(".answers[data-mc-worksheet-to-question-id=" + mcWorksheetToQuestionId + "]").hide();

		var thisChartOptions = $.extend(true, {}, chartOptions, {
			hAxis: {
				maxValue : maxCount,

				/* the number of gridlines has to match with
				 * the maximal count, otherwise we may get gridlines
				 * for	0.25, 0.5 etc. which doesn't make sense. */
				gridlines: {
					count: maxCount + 1
				},
			}
		});

		var chart = new google.visualization.BarChart($chartDiv.get(0));
		chart.draw(dataTable, thisChartOptions);
	}

	// load Google Charts API
	google.load(
		"visualization",
		"1.0", {
			callback: loadCharts,
			packages: ["corechart"]
		}
	);
});