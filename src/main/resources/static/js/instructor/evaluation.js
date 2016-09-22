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
	"use strict";

	var chartOptions = {
			backgroundColor: "transparent",
			legend: {
				position: "none"
			},
			hAxis: {
				titleTextStyle: {
					bold: true,
					italic: false
				}
			},
			vAxis: {
				titleTextStyle: {
					bold: true,
					italic: false
				},
				title: messages.count
			}
		};

		function loadCharts() {
			/* load the answer stats for each of the questions.
			 * Note that there are no ".answerChart" elements if the worksheet
			 * is not unlocked yet, so this is a NOOP in this case. */
			$(".choicesChart").each(function() {
				var $chartDiv = $(this);
				var evQuestionId = $chartDiv.data("ev-question-id");

				$.ajax({
					type : "GET",
					url : window.location.pathname + "/choiceStats/" + evQuestionId,

					success : function(evQuestionStats) {
						/* question stats is a EvaluationQuestionStats object.
						 * Initialize the chart with this data. */
						initChart(evQuestionId, evQuestionStats, $chartDiv);
					}
				});
			});
		}

		// initializes a chart for a specific question
		function initChart(evQuestionId, evQuestionStats, $chartDiv) {
			var dataTable = new google.visualization.DataTable();

			dataTable.addColumn("string", messages.choice);
			dataTable.addColumn("number", messages.count);

			// count of the answer that has been chosen the most
			var maxCount = 0;

			// iterate the answers with its stats
			evQuestionStats.choiceStats.forEach(function(entry) {
				var choice = entry.choice;
				var text;
				if (choice.text !== null && choice.value !== null)
					text = choice.text + " (" + choice.value + ")";
				else if (entry.choice.text === null)
					text = "(" + choice.value + ")";
				else
					text = choice.text;

				dataTable.addRow([text, entry.count]);

				if (entry.count > maxCount)
					maxCount = entry.count;
			});

			// this question hasn't been answered yet, don't show a chart
			if (maxCount === 0)
				return;

			// hide the answer list (it would be redundant since all the information is in the chart)
			$(".question[data-ev-question-id=" + evQuestionId + "] .choices").hide();

			var thisChartOptions = $.extend(true, {}, chartOptions, {
				vAxis: {
					maxValue : maxCount,

					/* the number of gridlines has to match with
					 * the maximal count, otherwise we may get gridlines
					 * for	0.25, 0.5 etc. which doesn't make sense. */
					gridlines: {
						count: maxCount + 1
					}
				}
			});

			if (evQuestionStats.averageValue !== null) {
				var avgStr = evQuestionStats.averageValue.toFixed(1);
				thisChartOptions.hAxis.title = messages.averageN.format(avgStr);
			}

			var chart = new google.visualization.ColumnChart($chartDiv.get(0));
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