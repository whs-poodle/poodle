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
	/* global exercise */
	/* global messages */
	/* global ckeditorConfig */
	"use strict";

	var MIN_ANSWERS = 2;
	var MAX_ANSWERS = 8;

	function addAnswerField(showEffect) {
		var $answerListe = $("ul#answerList");

		if(getAnswerCount() >= MAX_ANSWERS) {
			var msg = messages.tooManyAnswers.format(MAX_ANSWERS);
			utils.showErrorMessage(msg);
			return;
		}

		// the server sends us the markup for the input
		$.ajax({
			url : utils.contextPath + "instructor/mcQuestions/getAnswerInput",
			type : "GET",

			success : function(html) {
				var $answerListItem = $(html);

				// set correctSingle/Multiple status and enable "remove answer" link
				var isMultiple = isMultipleCorrectAnswers();
				$answerListItem.find(".correctSingle").prop("disabled", isMultiple);
				$answerListItem.find(".correctMultiple").prop("disabled", !isMultiple);

				$answerListItem.find(".removeAnswerLink").click(onRemoveAnswerClick);

				// add to List
				if (showEffect)
					$answerListItem.hide();

				$answerListe.append($answerListItem);

				if (showEffect)
					$answerListItem.slideDown();
			}
		});
	}

	function onRemoveAnswerClick() {
		/* jshint validthis: true */

		if (getAnswerCount() <= MIN_ANSWERS) {
			var msg = messages.tooFewAnswers.format(MIN_ANSWERS);
			utils.showErrorMessage(msg);
			return;
		}

		var $parentLi = $(this).closest("li");

		$parentLi.slideUp(function() {
			$(this).remove();
		});
	}


	function getAnswerCount() {
		return $("ul#answerList").children().length;
	}

	function submitForm($form) {
		/*
		 * Set the values of the correctSingle radio buttons and
		 * correctMultiple checkboxes to the corresponding indexes. We do
		 * this here since the list may change dynamically.
		 * The server matches the submitted indexes against the submitted
		 * answers and determines which answers were marked as "correct".
		 */
		$(".correctSingle").each(function(i) {
			$(this).val(i);
		});
		$(".correctMultiple").each(function(i) {
			$(this).val(i);
		});

		/* Make sure that all CKEditor instances update their
		 * textarea object, otherwise the form might contain
		 * old data. */
		for (var instance in CKEDITOR.instances)
			CKEDITOR.instances[instance].updateElement();

		var formData = $form.serialize();

		$.ajax({
			url: utils.contextPath + "instructor/mcQuestions/save",
			data: formData,
			type: "POST",

			// the server returns the id of the generated question
			success: function(json) {
				var id = json.id;
				utils.redirect("instructor/mcQuestions/" + id + "?saveSuccess=1");
			}
		});
	}

	function isMultipleCorrectAnswers() {
		return $("input[name=multipleCorrectAnswers]:checked").val() === 'true';
	}

	// load CKEditor
	var courseId = $("head").data("course-id");

	CKEDITOR.replace($("textarea#text")[0],
		ckeditorConfig.create(courseId, {
			startupFocus : true,
			height : "300px"
		}));

	// "add answer" button
	$("#addAnswerLink").click(function() {
		addAnswerField(true);
	});

	// confirm deletion
	$("#deleteForm").submit(function() {
		return exercise.confirmDelete();
	});

	/* Disable the radio buttons or checkboxes, depending
	 * on wich type is selected. They are hidden via CSS. */
	var $typeRadioButtons = $("input[name=multipleCorrectAnswers]:radio");
	$typeRadioButtons.change(function () {
		 var isMultiple = isMultipleCorrectAnswers();
		 $(".correctSingle").prop("disabled", isMultiple);
		 $(".correctMultiple").prop("disabled", !isMultiple);
	});
	$typeRadioButtons.trigger('change');

	// submit form via JavaScript (s. submitForm())
	$("#questionForm").submit(function(e) {
		e.preventDefault();
		submitForm($(this));
	});

	// "remove answer" buttons
	$(".removeAnswerLink").click(onRemoveAnswerClick);

	 // If a new question is created, add two empty answer fields.
	if (getAnswerCount() === 0) {
		addAnswerField(false);
		addAnswerField(false);
	}
});