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

	function isTagFilterEnabled() {
		return $("input[name=enableTagFilter]:checked").val() === 'true';
	}

	function reloadQuestionCount() {
		var $questionCountSpan = $("span#questionCount");
		var $form = $("form#createWorksheetForm");

		var $notEnoughQuestionsMessage = $("#notEnoughQuestionsMessage");
		var $createWorksheetButton = $("#createWorksheetButton");

		var formData = $form.serialize();

		// make sure we don't send an invalid number to the server i.e. ignore them
		var maximum = $("#maximum").val();
		if (!$.isNumeric(maximum) || maximum < 1)
			return;

		$.ajax({
			url: window.location.pathname + "/mcQuestionCount",
			type: "GET",
			data : formData,

			success: function(json) {
				var count = json.count;

				var hasQuestions = count > 0;
				$notEnoughQuestionsMessage.toggle(!hasQuestions);
				$createWorksheetButton.prop("disabled", !hasQuestions);

				var countStr = messages.nQuestions(count);
				$questionCountSpan.html(countStr);
			}
		});
	}

	var $tagsWrapper = $("#tagsWrapper");

	// Show/hide the tag list, depending on whether "filter by tags" is enabled or not.
	var $tagModeRadioButtons = $("input[name=enableTagFilter]:radio");
	$tagModeRadioButtons.change(function () {
		 if (isTagFilterEnabled())
			 $tagsWrapper.slideDown();
		 else
			 $tagsWrapper.slideUp();
	});
	$tagsWrapper.toggle(isTagFilterEnabled());

	// load exercise count for the chosen criteria
	reloadQuestionCount();

	// reload the question count every time the form changes
	$("#createWorksheetForm input").change(reloadQuestionCount);
});