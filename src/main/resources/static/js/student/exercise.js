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
	"use strict";

	// reset feedback form and submit it on clock in "no feedback"
	$(".noFeedbackLink").click(function() {
		var exerciseId = $(this).data("exercise-id");
		var $form = $("#feedback" + exerciseId + ' form');
		$form[0].reset();
		$form.submit();
	});

	// show next hint on click on "give hint"
	$(".giveHintLink").click(function() {
		var $hiddenHints = $(this).parent().siblings(".hint:hidden");
		$hiddenHints.first().slideDown();

		// last hint unlocked, hide link
		if ($hiddenHints.length === 1)
			$(this).slideUp();
	});
});