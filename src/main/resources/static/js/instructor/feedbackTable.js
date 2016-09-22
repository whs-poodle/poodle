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
	/* global ckeditorConfig */
	/* global utils */
	/* global statisticUtils */
	"use strict";

	/* Initialize the CKEditor as soon as the popover is visible. We
	 * can not do it before because the CKEditor would simply not be visible. */
	$(".toggleCommentFormButton").on('shown.bs.popover', function () {
		var $button = $(this);
		// Bootstrap always creates the popover next to the triggering element
		var $form = $button.next(".popover").find("form");

		var $commentTextarea = $form.find("textarea[name=comment]");

		/* we have to make the CKEditor exactly as wide the form itself.
		 * Otherwise the popover has to resize which screws up its position. */
		var ckeditorWidth = $form.css("width");

		var courseId = $button.data("course-id");

		CKEDITOR.replace($commentTextarea[0],
			ckeditorConfig.create(courseId, {
			height : "300px",
			width : ckeditorWidth
		}));

		$form.submit(function(e) {
			e.preventDefault();

			/* disable the button since sending the email could take a second
			 * and we don't want the instructor to click again. */
			var $submitButton = $form.find(".submitButton");
			$submitButton.prop("disabled", true);

			// force CKEditor to update the textarea element to make sure the data is up to date
			CKEDITOR.instances.comment.updateElement();

			$.ajax({
				url: utils.contextPath + "instructor/rest/commentOnFeedback",
				method : "POST",
				data: $form.serialize(),

				success: function(json) {
					var statisticId = $form.find("input[name=statisticId]").val();
					var comment = $form.find("textarea[name=comment]").val();

					$button.popover("hide");

					// set the text on the "already commented" popup and swap the icons
					var $isNotCommentedWrapper = $("#isNotCommentedWrapper" + statisticId);
					var $isCommentedWrapper = $("#isCommentedWrapper" + statisticId);
					var $commentTextDiv = $isCommentedWrapper.find(".commentText");

					$isNotCommentedWrapper.hide();
					$commentTextDiv.html(comment);
					$isCommentedWrapper.show();

					utils.showOkMessage(json.message);
				},

				error: function() {
					$submitButton.prop("disabled", false);
				}
			});
		});
	});

	// ignore a specific statistic
	function setIgnoreFeedback(statisticId, ignore) {
		/* note that on the exercise page (/instructor/exercises/id) we re-use
		 * the <thead> and <tbody> of the feedbackTable but also have a <tfoot>
		 * with the average values. So if there is a <tfoot> we also
		 * have to update the average values. */
		var $tfoot = $("#feedbackTable tfoot");
		var hasAvgValues = $tfoot.length !== 0;

		// sets whether the server has to return the new average values
		var avgValueContext = hasAvgValues ? "ALL" : "NONE";

		statisticUtils.setIgnoreStatisticAjax(statisticId, ignore, avgValueContext)
		.done(function(json) {
			/* set data-ignore on the table row. The CSS makes sure it is marked accordingly
			 * and the correct ignore/dont ignore button is displayed. */
			var $tr = $("tr[data-statistic-id=" + statisticId + "]");
			$tr.attr("data-ignore", ignore);

			// update the average values on the footer, if we have to
			if (hasAvgValues) {
				$tfoot.find(".avgDifficulty").html(json.avgDifficultyStr);
				$tfoot.find(".avgFun").html(json.avgFunStr);
				$tfoot.find(".avgTime").html(json.avgTimeStr);
			}

			utils.showOkMessage(json.message);
		});
	}

	$(".ignoreFeedbackButton").click(function() {
		var statisticId = $(this).data("statistic-id");
		setIgnoreFeedback(statisticId, true);
	});

	$(".dontIgnoreFeedbackButton").click(function() {
		var statisticId = $(this).data("statistic-id");
		setIgnoreFeedback(statisticId, false);
	});
});