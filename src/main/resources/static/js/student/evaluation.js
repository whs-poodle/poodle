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
	/* global utils */
	"use strict";

	/*
	 * We don't use a simple form to send the evaluation
	 * since the data is too complex for key-value-pairs.
	 * Instead, we build a JSON object of the chosen answers
	 * matching the StudentEvaluationData Java class.
	 */
	function sendEvaluation() {
		var evaluationData = {
			questionChoices: []
		};

		var $questionDivs = $(".question");

		/* iterate all questions to make sure all of
		 * them were answered. */
		$questionDivs.each(function() {
			var $questionDiv = $(this);
			var questionId = $questionDiv.data("question-id");
			var questionType = $questionDiv.data("question-type"); // CHOICE or TEXT

			// checked choice (undefined if none checked or question is of type text)
			var choiceId = $(this).find("input[type=radio]:checked").val();

			// question was not answered
			if (questionType === "CHOICE" && choiceId === undefined) {
				utils.showErrorMessage(messages.missingChoice);

				// mark the question that wasn't answered with a warning border...
				$questionDiv.addClass("missingChoice");

				// ... and scroll to it

				// we have subtract the body padding since our navigation is fixed
				var bodyPaddingTop = parseInt($("body").css("padding-top"));
				var scrollPos = $questionDiv.offset().top - bodyPaddingTop;

				$('html,body').animate({
				   scrollTop: scrollPos
				});

				return false; // break loop
			}

			// build the choice for this question (StudentEvQuestionChoice java class)
			var choice = {
				evaluationQuestionId : questionId
			};

			if (questionType === "CHOICE") {
				choice.choiceId = parseInt(choiceId);
			}
			else {
				var text = $(this).find("textarea.questionText").val();
				choice.text = text;
			}

			evaluationData.questionChoices.push(choice);
		});

		// not all questions were processed, abort (at least one question was not answered)
		if ($questionDivs.length !== evaluationData.questionChoices.length)
			return;

		// send the data to the server
		$.ajax({
			url: window.location.pathname,
			type: "POST",
			data: JSON.stringify(evaluationData),
			contentType: 'application/json',

			success : function() {
				// evaluation saved successfully, redirect to start page
				utils.redirect("student?evaluationSaved=1");
			}
		});
	}

	// remove the "not answered" warning border if an answer is chosen
	$(".question input[type=radio]").click(function() {
		$(this).closest(".question").removeClass("missingChoice");
	});

	$("#sendEvaluationButton").click(sendEvaluation);
});