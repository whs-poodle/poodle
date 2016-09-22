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
	/* global exercise */
	/* global utils */
	"use strict";

	/* Used to generate a download link after a file has been uploaded */
	function createDownloadLink(inputName, fileId, filename) {
		var downloadLinkTemplate = $("#downloadLinkTemplate").html();
		Mustache.parse(downloadLinkTemplate);

		return Mustache.render(downloadLinkTemplate, {
			inputName : inputName,
			fileId : fileId,
			filename : filename
		});
	}

	function loadCKEditor() {
		/* global ckeditorConfig */
		var courseId = $("head").data("course-id");

		CKEDITOR.replace($("textarea#text")[0],
			ckeditorConfig.create(courseId, {
				height: "300px"
		}));
		CKEDITOR.replace($("textarea#hint1")[0],
			ckeditorConfig.create(courseId, {
				height: "200px"
		}));
		CKEDITOR.replace($("textarea#hint2")[0],
			ckeditorConfig.create(courseId, {
				height: "200px"
		}));
		CKEDITOR.replace($("#sampleSolutionText textarea")[0],
			ckeditorConfig.create(courseId, {
				height: "200px"
		}));
	}

	loadCKEditor();

	function setupSampleSolution() {
		// Show the CKEditor or the file button, depending on which
		// radio button ("none", "file", "text") is checked.
		var $sampleSolutionTypRadios = $("input[name='sampleSolutionType']");

		$sampleSolutionTypRadios.change(function() {
			var $fileDiv = $("#sampleSolutionFile");
			var $textDiv = $("#sampleSolutionText");

			var typ = $sampleSolutionTypRadios.filter(":checked").val();

			if (typ == 'NONE') {
				$fileDiv.slideUp();
				$textDiv.slideUp();
			}
			else if (typ == 'FILE') {
				$fileDiv.slideDown();
				$textDiv.slideUp();
			}
			else { // Text
				$fileDiv.slideUp();
				$textDiv.slideDown();
			}
		});

		$sampleSolutionTypRadios.trigger("change");
	}

	setupSampleSolution();

	// confirm exercise deletion
	$("#deleteForm").submit(function() {
		return exercise.confirmDelete();
	});

	// trigger the file input element on click on "choose file"
	$("#sampleSolutionFileSelectLink").click(function() {
		var $sampleSolutionInput = $("#sampleSolutionFileInput");
		$sampleSolutionInput.click();
	});

	// when a file has been chosen, upload it and create a download link
	$("#sampleSolutionFileInput").change(function() {
		var file = $(this).prop("files")[0];

		// server sends us the file id, write it into the id input field
		utils.uploadFile(file).done(function(json) {
			var downloadLink = createDownloadLink("sampleSolutionFileId", json.id, file.name);
			$("#sampleSolutionInfo").html(downloadLink);
		});
	});

	// trigger the file input element on click in "add files"
	$("#selectAttachmentsLink").click(function() {
		var $attachmentsInput = $("#attachmentsInput");
		$attachmentsInput.click();
	});

	// wee attachment(s) have been chosen, upload them and generate the download link
	$("#attachmentsInput").change(function() {
		var files= $(this).prop("files");

		$.each(files, function(i, file) {
			// server sends us back the file id, generiere an ID input field
			utils.uploadFile(file).done(function(json) {
				var downloadLink = createDownloadLink("attachmentIds", json.id, file.name);
				var $newLi = $("<li>").html(downloadLink);
				$("#attachmentList").append($newLi);
			});
		});
	});

	/*
	 * Remove solution / attachments on click on the "remove" buttons.
	 * The files are "deleted" in the save revision since we remove the
	 * IDs from the hidden input fields.
	 */
	$(".removeSampleSolutionButton").click(function() {
		$("#sampleSolutionInfo").empty();
	});
	$(".removeAttachmentButton").click(function() {
		$(this).parent().remove(); // parent is the <li> element
	});

	// save Exercise
	$("#exerciseForm").submit(function(e) {
		e.preventDefault();

		/* make sure all CKEditor instances update
		 * their <textarea> objects, otherwise the form
		 * may contain old data. */
		for (var instance in CKEDITOR.instances)
			CKEDITOR.instances[instance].updateElement();

		$.ajax({
			url: utils.contextPath + "instructor/exercises/save",
			data: $(this).serialize(),
			type: "POST",

			// the server sends back the generated id
			success: function(json) {
				var id = json.id;
				utils.redirect("instructor/exercises/" + id + "?saveSuccess=1");
			}
		});
	});
});