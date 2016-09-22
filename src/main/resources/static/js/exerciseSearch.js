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

	function deleteExercise(exerciseId) {
		$.ajax({
			url: utils.contextPath + "instructor/rest/exercises/" + exerciseId,
			type: "DELETE",

			success: function() {
				utils.showOkMessage(messages.exerciseDeleted);
				exerciseSearch.removeRow(resultsTable, exerciseId);
			}
		});
	}

	function addExerciseToSelfStudyWorksheet(exerciseId, courseTermId) {
		$.ajax({
			url: window.location.pathname,
			data: {
				"courseTermId" : courseTermId,
				"exerciseId" : exerciseId
			},
			type: "POST",

			success: function(json) {
				/* "left" is the number of exercises the student can still
				 * put on his sheet. */
				var exercisesLeft = json.left;

				if (exercisesLeft === 0) {
					// sheet is full, redirect to worksheet
					utils.redirect("student/selfStudy/" + courseTermId);
					return;
				}

				var msg = messages.nExercisesLeft(exercisesLeft);

				utils.showOkMessage(msg);

				exerciseSearch.removeRow(resultsTable, exerciseId);
			}
		});
	}

	function addExerciseToChapter(exerciseId, chapterId) {
		$.ajax({
			url: window.location.pathname + "/addExerciseToChapter",
			data: {
				"exerciseId" : exerciseId,
				"chapterId" : chapterId
			},
			type: "POST",

			success: function() {
				utils.showOkMessage(messages.exerciseAdded);
				exerciseSearch.removeRow(resultsTable, exerciseId);
			}
		});
	}

	// hide the ascending/descending buttons if RANDOM is selected
	$('select#order').change(function() {
		var ascDescEnabled = this.value != 'RANDOM';
		$('#orderAscendingWrapper').toggle(ascDescEnabled);
	});
	$('select#order').trigger('change');

	// instructors: delete exercise on click on link
	$(".deleteExerciseLink").click(function() {
		var $link = $(this);
		var title = $link.data("title");
		var exerciseId = $link.data("exercise-id");

		var yes = confirm(messages.reallyDeleteExercise.format(title));

		if (yes)
			deleteExercise(exerciseId);
	});

	// students: add exercise to self-study sheet
	$(".addToSelfStudyWorksheetLink").click(function() {
		var $link = $(this);
		var exerciseId = $link.data("exercise-id");
		var courseTermId = $link.data("course-term-id");

		addExerciseToSelfStudyWorksheet(exerciseId, courseTermId);
	});

	// instructors: add exercise to chapter of worksheet
	$(".addToChapterLink").click(function() {
		var $link = $(this);
		var exerciseId = $link.data("exercise-id");
		var chapterId = $link.data("chapter-id");

		addExerciseToChapter(exerciseId, chapterId);
	});
});