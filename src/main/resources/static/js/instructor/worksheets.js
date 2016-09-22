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

	// confirm "delete worksheet"
	$('.deleteForm').submit(function() {
		var title = $(this).data('title');
		var msg = messages.reallyDeleteWorksheet.format(title);
		return confirm(msg);
	});

	// confirm "unlock worksheet"
	$('.unlockForm').submit(function() {
		var title = $(this).data('title');
		var msg = messages.reallyUnlockWorksheet.format(title);
		return confirm(msg);
	});

	// show/hide "unlock at" form
	function toggleUnlockAtForm($unlockAtButton) {
		var $parentTd = $unlockAtButton.closest("td");
		var $optionButtons = $parentTd.find(".optionButtons");
		var $unlockAtForm = $parentTd.find(".unlockAtForm");

		$optionButtons.slideToggle();
		$unlockAtForm.slideToggle();
	}

	$(".unlockAtButton").click(function() {
		toggleUnlockAtForm($(this));
	});

	$(".unlockAtCancelButton").click(function() {
		toggleUnlockAtForm($(this));
	});

	/* initialize DateTimePickers.
	/* https://eonasdan.github.io/bootstrap-datetimepicker/Options/ */
	$(".datetimepicker").each(function() {
		/* We need to define our own widgetParent (the wrapping div in the HTML)
		 * because the positioning screws up otherwise. */
		var $parent = $(this).parent();

		$(this).datetimepicker({
			locale : messages.dateTimePickerLocale,
			format : messages.dateTimePickerFormat,
			stepping: 5, // minute steps
			minDate: moment(), // now
			sideBySide : true, // always show both date and time pickers
			useStrict : true, // be strict about the date format
			widgetParent : $parent
		});
	});

	// button to clear the time input, i.e. to disable an already set time
	$(".clearDateTimeButton").click(function() {
		var $dateTimePicker = $(this).prev().find(".datetimepicker");
		$dateTimePicker.val("");
	});
});