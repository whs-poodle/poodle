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
 * This code is mostly responsible for creating the
 * diagrams with the Google Charts API.
 *
 * The server sends us the statistics as a 2 dimensional JSON array.
 * This way we can directly use it with Google's dataTable.addRows()
 * function. All diagrams are created from this table.
 *
 * Column 0: completion status (partly, not at all...)
 * Column 1: Difficulty
 * Column 2: Fun
 * Column 3: Time
 *
 * The difficulty, fun and completion status diagrams are Column Charts,
 * the time diagrams is a Histogram.
 *
 * https://developers.google.com/chart/
 * https://developers.google.com/chart/interactive/docs/gallery/columnchart
 * https://developers.google.com/chart/interactive/docs/gallery/histogram
 */
$(document).ready(function() {
	/* global exercise */
	/* global messages */
	"use strict";

	/* Minimum amount of data we need to create a diagram.
	 * If we have less than this, the diagram is not rendered and
	 * the tab is disabled. */
	var MIN_DATA_COUNT = 2;

	// global options for all diagrams
	var OPTIONS_ALL = {
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
			title: messages.count,
			minValue: 0
		}
	};

	var $chartsTabs = $("#chartsTabs");

	function loadChartData() {
		$.ajax({
			url: window.location.pathname + "/chartData",
			type: "GET",
			success: onChartDataLoaded
		});
	}

	function onChartDataLoaded(data) {
		// create dataTable
		var dataTable = new google.visualization.DataTable();
		dataTable.addColumn("string", messages.completed);
		dataTable.addColumn("number", messages.difficulty);
		dataTable.addColumn("number", messages.fun);
		dataTable.addColumn("number", messages.time);
		dataTable.addRows(data);

		/* Create all charts.
		 * We are doing this _before_ creating the tabs on purpose,
		 * since the API is not able to render into a hidden
		 * div i.e. inactice tab.
		 *
		 * Every draw function returns whether the diagram was created or
		 * not (MIN_DATA_COUNT). If not, we disable the tab on this index.
		 */
		var disabledTabs = [];
		if (!drawDifficultyChart(dataTable))
			disabledTabs.push(0);
		if (!drawTimeChart(dataTable))
			disabledTabs.push(1);
		if (!drawFunChart(dataTable))
			disabledTabs.push(2);
		if (!drawCompletedChart(dataTable))
			disabledTabs.push(3);

		if ($("#textList > li").length === 0)
			disabledTabs.push(4);

		var tabCount = $chartsTabs.find("#tabList > li").length;

		// all tabs disabled, hide them and abort
		if (disabledTabs.length === tabCount) {
			$chartsTabs.hide();
			return;
		}

		// get index of the first tab that is not disabled
		var activeTab = 0;
		for (var i = 0; i < tabCount; i++) {
			if ($.inArray(i, disabledTabs) === -1) {
				activeTab = i;
				break;
			}
		}

		// generate tabs
		$chartsTabs.tabs({
			disabled: disabledTabs,
			active: activeTab
		});
	}

	function drawDifficultyChart(dataTable) {
		var $difficultyChart =$("#difficultyChart");
		var avgDifficulty = $difficultyChart.data("avg");

		var difficultyOptions = $.extend(true, {}, OPTIONS_ALL, {
			hAxis: {
				title: messages.difficultyTitle.format(avgDifficulty)
			}
		});

		var counts = new google.visualization.DataTable();
		// this column must be of type string. Otherwise not all values are displayed on the x axis.
		counts.addColumn("string", messages.difficulty);
		counts.addColumn("number", messages.count);

		var dataCount = 0;
		for (var difficulty = 1; difficulty <= 10; difficulty++) {
			var count = dataTable.getFilteredRows([{column: 1, value: difficulty}]).length;
			counts.addRow([difficulty.toString(), count]);
			dataCount += count;
		}

		if (dataCount < MIN_DATA_COUNT)
			return false;

		var chart = new google.visualization.ColumnChart($difficultyChart.get(0));
		chart.draw(counts, difficultyOptions);
		return true;
	}

	function drawTimeChart(dataTable) {
		var $timeChart = $("#timeChart");
		var avgTime = $timeChart.data("avg");

		var view = new google.visualization.DataView(dataTable);
		view.setRows(view.getFilteredRows(
				[{
					column: 3, // only columns with time >= 1
					minValue: 1
				}])
		);
		view.setColumns([3]); // only time column

		if (view.getNumberOfRows() < MIN_DATA_COUNT)
			return false;


		var timeOptions = $.extend(true, {}, OPTIONS_ALL, {
			hAxis: {
				title: messages.timeTitle.format(avgTime)
			}
		});

		var chart = new google.visualization.Histogram($timeChart.get(0));
		chart.draw(view, timeOptions);
		return true;
	}

	function drawFunChart(dataTable) {
		var $funChart = $("#funChart");
		var avgFun = $funChart.data("avg");

		var funOptions = $.extend(true, {}, OPTIONS_ALL, {
			hAxis: {
				title: messages.funTitle.format(avgFun)
			}
		});

		var counts = new google.visualization.DataTable();
		// this column must be of type string. Otherwise not all values are displayed on the x axis.
		counts.addColumn("string", messages.fun);
		counts.addColumn("number", messages.count);

		var dataCount = 0;
		for (var fun = 1; fun <= 10; fun++) {
			var count = dataTable.getFilteredRows([{column: 2, value: fun}]).length;
			counts.addRow([fun.toString(), count]);
			dataCount += count;
		}

		if (dataCount < MIN_DATA_COUNT)
			return false;

		var chart = new google.visualization.ColumnChart($funChart.get(0));
		chart.draw(counts, funOptions);
		return true;
	}

	function drawCompletedChart(dataTable) {
		var counts = new google.visualization.DataTable();
		counts.addColumn("string", messages.completed);
		counts.addColumn("number", messages.count);

		var dataCount = 0;

		/* messages.completedStatus contains the Java enum values (which are also
		 * in the dataTable) as the keys and the localized description as the values.
		 * Iterate over the keys and add a row for each. */
		var completedStatus = messages.completedStatus;

		for (var s in completedStatus) {
			var count = dataTable.getFilteredRows([{column: 0, value: s}]).length;
			counts.addRow([completedStatus[s], count]);
			dataCount += count;
		}

		if (dataCount < MIN_DATA_COUNT)
			return false;

		var completedOptions = $.extend(true, {}, OPTIONS_ALL, {
			hAxis: {
				title: messages.completed
			}
		});

		var chart = new google.visualization.ColumnChart(document.getElementById("completedChart"));
		chart.draw(counts, completedOptions);
		return true;
	}

	// confirm exercise deletion
	$("#deleteForm").submit(function() {
		return exercise.confirmDelete();
	});

	/*
	 * Load diagram, if statistics exist.
	 * (#chartsTabs doesn't exist if the statistics are empty).
	 */
	if ($chartsTabs.length > 0) {
		google.load(
			"visualization",
			"1.0", {
				callback: loadChartData,
				packages: ["corechart"]
			}
		);
	}

	// initialize DataTables for feedback table
	$("#feedbackTable").DataTable({
		"order": [[ 1, "desc" ]] // date descending
	});
});