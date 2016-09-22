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
	/* global messages */
	"use strict";

	// confirm "cancel worksheet"
	$("#cancelForm").submit(function() {
		return confirm(messages.reallyCancelWorksheet);
	});

	// check whether an answer is selected on click on "next"
	$("#answerForm").submit(function() {
		var hasAnswer = $("input[name=answers]:checked").length > 0;

		if (!hasAnswer) {
			utils.showErrorMessage(messages.noAnswerSelected);
			return false;
		}

		/* Disable the button because the server "switches" to the next
		 * question after the first request and any further request would cause
		 * an internal server error since the answer IDs don't match
		 * the next question. */
		$("button[type=submit]").prop("disabled", true);

		return true;
	});
});