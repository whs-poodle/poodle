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
// common code for exerciseEditor and mcQuestionEditor

$(document).ready(function() {
	/* global utils */
	/* global messages */
	/* global createTagForm */
	"use strict";

	function createTag(tag) {
		createTagForm.createTag(tag)
			.done(function(tag) {
				/* create a new element for the tag list (matching tagFilter.html #tagCheckboxes) */
				var $checkbox = $("<input>")
								.prop("type", "checkbox")
								.prop("name", "tags")
								.prop("checked", true)
								.prop("value", tag.id);

				var $text = $("<span>")
							.text(tag.name); // escape

				var $label = $("<label>")
							 .append($checkbox)
							 .append($text);

				var $newDiv = $("<div>").addClass("checkbox").append($label);

				// add new element to list
				$("#tagCheckboxes").append($newDiv);

				// empty input field
				$("#newTagName").val("");

				utils.showOkMessage(messages.tagCreated);
			});
	}

	/* Override the enter key for the tag name input field in order
	 * to create the tag instead of submitting the outer form. */
	$("#newTagName").keypress(function(e) {
		if (e.which == 13) { // 13 = Enter
			var name = $(this).val();
			var courseId = $("#courseId").val();
			createTag(name, courseId);
			e.preventDefault();
		}
	});

	// "create tag" button
	$("#createTagButton").click(function(e) {
		var tag = {
			name : $("#newTagName").val(),
			courseId : $("#courseId").val(),
			instructorOnly : $("#instructorOnly").prop("checked")
		};

		createTag(tag);
		e.preventDefault();
	});
});