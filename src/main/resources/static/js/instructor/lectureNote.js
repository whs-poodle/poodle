/*
 * Copyright 2016 Westfälische Hochschule
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
	"use strict";

	// confirm "delete note"
	$('.deleteForm').submit(function() {
		var title = $(this).data('title');
		var msg = messages.reallyDeleteWorksheet.format(title);
		return confirm(msg);
	});

	$("#addButton").click(function() {
		if($("#drop").val() != "" && $("#lectureGroup").val() == "") {
			$("#lectureGroup").val($("#drop").val());
		}

		if( $("#lectureNoteFile").val() != "" && $("#newLectureNote").val() != "" && $("#lectureGroup").val() != "") {
			var file = $("#lectureNoteFile").prop("files")[0];

			// server sends us the file id, write it into the id input field
			utils.uploadFile(file).done(function(json) {
				$("#lectureNoteFileId").val(json.id);
				if($("#lectureGroup").val == "")
					$("#lectureGroup").val($("#drop").val());
				$("#newNote").find('[type="submit"]').trigger('click');
			});
		}
		else
			$("#newNote").find('[type="submit"]').trigger('click');
	});

	$(".renameButton").click(function() {
		toggleRenameForm($(this));
	});

	$(".renameCancelButton").click(function() {
		toggleRenameForm($(this));
	});

	$(".editFileButton").click(function() {
		toggleEditFileForm($(this));
	});

	$(".editFileCancelButton").click(function() {
		toggleEditFileForm($(this));
	});

	$(".submitEditFileButton").click(function() {
		editFile($(this));
	});

	// show/hide "rename" form
	function toggleRenameForm($renameButton) {
		var $parentTd = $renameButton.closest("td");
		var $optionButtons = $parentTd.find(".optionButtons");
		var $renameForm = $parentTd.find(".renameForm");

		$optionButtons.slideToggle();
		$renameForm.slideToggle();
	}
	// show/hide "edit" form
	function toggleEditFileForm($editFileButton) {
		var $parentTd = $editFileButton.closest("td");
		var $optionButtons = $parentTd.find(".optionButtons");
		var $editFileForm = $parentTd.find(".editFileForm");

		$optionButtons.slideToggle();
		$editFileForm.slideToggle();
	}

	function editFile($editFileButton) {
		var $parentTd = $editFileButton.closest("td");
		var $submitButton = $parentTd.find('[id="editFile"]');
		var $newFile = $parentTd.find('[id="newFile"]');
		var $newFileId = $parentTd.find('[id="newLectureNoteFileId"]');

		if( $newFile.val() != "" ) {
			var file = $newFile.prop("files")[0];
			// server sends us the file id, write it into the id input field
			utils.uploadFile(file).done(function(json) {
				$newFileId.val(json.id);
				$submitButton.trigger('click');
			});
		}
		else
		$submitButton.trigger('click');
	}
});