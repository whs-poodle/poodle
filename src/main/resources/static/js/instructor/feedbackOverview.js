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
	/* global utils */
	/* global statisticUtils */
	"use strict";

	// submit form on change
	var $form = $("#statsForm");
	$form.find("select").change(function() {
		$form.submit();
	});

	// Templates we use to generate the cell content
	var studentCellTemplate = $("#studentCellTemplate").html();
	Mustache.parse(studentCellTemplate);

	var valueCellTemplate = $("#valueCellTemplate").html();
	Mustache.parse(valueCellTemplate);

	// initialize DataTables
	var $statsTable = $("#statsTable");

	// DataTables has to send these with each request
	var courseTermId = $("head").data("course-term-id");
	var value = $("head").data("value");

	var table = $statsTable.DataTable({
		"autoWidth": false, // avoid using the whole width when we only show one worksheet
		"order": [[ 0, "asc" ]], // student ascending
		iDisplayLength: 25,
		"orderMulti": false,
		"serverSide": true,

		// Ajax configuration. https://datatables.net/reference/option/ajax
		"ajax" : {
			"url" : "feedbackOverview/tableData",

			/* Modify the request that is sent to the server so that
			 * it matches FeedbackOverviewDataTablesRequest. */
			"data": function (d) {
				/* We don't need the column data and it actually causes problems
				 * because we may have a lot of columns which generate so many
				 * GET parameters that the URL length limit is exceeded and the
				 * server can't process the request. */
				d.columns = {};

				// index of the column to sort by (only one can be set since we set orderMulti=false)
				var orderColumnIndex = d.order[0].column;
				// asc/desc (toUpperCase to match the Java Enum)
				d.orderDirection = d.order[0].dir.toUpperCase();

				/* Column 0 is the student column. In this case we don't
				 * set anything and the server will assume "order by student".
				 * Otherwise we add the root ID of the exercise to sort by to the request. */
				if (orderColumnIndex !== 0) {
					// get exercise root ID to sort by from the header and add it to the request
					var exerciseRootId = $("th.exerciseHeader")
										.eq(orderColumnIndex - 1)
										.data("exercise-root-id");

					d.orderByExerciseRootId = exerciseRootId;
				}

				d.courseTermId = courseTermId;
				d.value = value;
			}
		},

		/* Define how the columns are rendered.
		 * We use a mustache.js template to render the contents
		 * for the cells based on the data sent by the server.
		 * https://datatables.net/reference/option/columns.render */
		"columnDefs": [
			   // student cell
			   {
				   "targets": "studentHeader",
				   "render": function(student) {
					   return Mustache.render(studentCellTemplate, {
						   student : student
					   });
				   }
			   },

			   // value cell
			   {
				   "targets": "exerciseHeader",
				   "render": function(valueCell) {
					   return Mustache.render(valueCellTemplate, {
						  statistic : valueCell.statistic,
						  color : valueCell.cssColor,
						  displayValue : valueCell.displayValue
					   });
				   }
			   }
		],

		// re-initialize the popovers each time the cells change
		fnDrawCallback : reinitCells
	});

	// initialize the popovers and tooltips on the cells
	function reinitCells() {
		var $valueSpans = $statsTable.find("span.value");

		$valueSpans.click(function() {
			var $valueSpan = $(this);

			// make sure we initialize the popover only once
			var isAlreadyPopover = $valueSpan.hasClass("isPopover");
			if (isAlreadyPopover)
				return;

			var statisticId = $valueSpan.data("statistic-id");

			// get the popover content from the server.
			$.ajax({
				url : "feedbackOverview/popoverContent/" + statisticId,

				// initialize the popover and open it
				success : function(popoverContent) {
					$valueSpan.addClass("isPopover");

					$valueSpan.popover({
						html: true,
						content : popoverContent
					});

					$valueSpan.popover("show");
				}
			});
		});

		/* Initialize the "ignoreFeedback" button. We have to do this
		 * after the popover has been opened because the actual DOM elements
		 * are created by Bootstrap. */
		$valueSpans.on("shown.bs.popover", function() {
			var $valueSpan = $(this);

			// Bootstrap always creates the popover div next to the object initializing it
			var $popover = $valueSpan.next(".popover");

			var $ignoreFeedbackCheckbox = $popover.find(".ignoreFeedbackCheckbox");

			$ignoreFeedbackCheckbox.change(function() {
				var ignore = $(this).is(":checked");
				setIgnoreFeedback($valueSpan, ignore);
			});
		});
	}

	function showSingleWorksheet(worksheetId) {
		/* Hide all columns not belonging to this worksheet.
		 * We also have to match whether data-worksheet-id actually exists,
		 * otherwise we would hide the student column as well.
		 * The second parameter tells DataTables to not waste time recalculating
		 * the cell sizes since we do another visible() call anyway. */
		table.columns("[data-worksheet-id][data-worksheet-id!=" + worksheetId + "]").visible(false, false);

		// show all columns for this worksheet
		table.columns("[data-worksheet-id=" + worksheetId + "]").visible(true);
	}

	function showAllWorksheets() {
		table.columns().visible(true);
	}

	$(".showOnlyThisWorksheetButton").click(function() {
		var $this = $(this);
		var worksheetId = $this.data("worksheet-id");
		showSingleWorksheet(worksheetId);

		$this.hide();
		$(".showAllWorksheetsButton[data-worksheet-id=" + worksheetId + "]").show();
	});

	$(".showAllWorksheetsButton").click(function() {
		showAllWorksheets();

		var $this = $(this);
		var worksheetId = $this.data("worksheet-id");
		$this.hide();
		$(".showOnlyThisWorksheetButton[data-worksheet-id=" + worksheetId + "]").show();
	});

	function setIgnoreFeedback($span, ignore) {
		var statisticId = $span.data("statistic-id");

		statisticUtils.setIgnoreStatisticAjax(statisticId, ignore, "WORKSHEET")
			.done(function(json) {
				// close popover
				$span.popover("hide");

				// callback after the table has been reloaded (see below)
				function onTableReloaded() {
					/* Update the average value (note that the footer doesn't exist for
					 * "completed" which makes this a noop). */
					var $valueSelection = $("#valueSelection");
					var value = $valueSelection.val();
					var newAvgStr;

					switch (value) {
					case "TIME":
						newAvgStr = json.avgTimeStr;
						break;

					case "DIFFICULTY":
						newAvgStr = json.avgDifficultyStr;
						break;

					case "FUN":
						newAvgStr = json.avgFunStr;
						break;
					}

					if (newAvgStr)
						$statsTable.find("tfoot>tr>td[data-root-id=" + json.exerciseRootId + "]").html(newAvgStr);

					utils.showOkMessage(json.message);
				}

				/* reload the table (only the body, the footer is reloaded in the callback)
				 * We don't simply mark the value as ignored since the colors for the other cells
				 * may have changed as well (e.g. if the max time value is ignored). */
				table.ajax.reload(onTableReloaded, false);
			});
	}
});