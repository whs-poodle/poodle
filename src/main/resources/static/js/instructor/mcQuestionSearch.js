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
	/* global exerciseSearch */
	/* global messages */
	"use strict";

	var resultsTable = exerciseSearch.initResultsTable();

	function deleteQuestion(questionId) {
		$.ajax({
			url: utils.contextPath + "instructor/rest/mcQuestions/" + questionId,
			type: "DELETE",

			success: function() {
				utils.showOkMessage(messages.questionDeleted);
				exerciseSearch.removeRow(resultsTable, questionId);
			}
		});
	}

	// confirm delete question
	$(".deleteQuestionLink").click(function() {
		var $link = $(this);
		var title = $link.data("title");
		var questionId = $link.data("question-id");

		var yes = confirm(messages.reallyDeleteExercise.format(title));

		if (yes)
			deleteQuestion(questionId);
	});

	function addQuestionToMcWorksheet(mcWorksheetId, questionId) {
		$.ajax({
			url: window.location.pathname + "/addQuestionToWorksheet",
			data: {
				"mcWorksheetId" : mcWorksheetId,
				"mcQuestionId" : questionId
			},
			type: "POST",

			success: function(json) {
				utils.showOkMessage(json.message);
				exerciseSearch.removeRow(resultsTable, questionId);
			}
		});
	}

	$(".addToMcWorksheetLink").click(function() {
		var mcWorksheetId = $(this).data("mc-worksheet-id");
		var questionId = $(this).data("question-id");
		addQuestionToMcWorksheet(mcWorksheetId, questionId);
	});
});