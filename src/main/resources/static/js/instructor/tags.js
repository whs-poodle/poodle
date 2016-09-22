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
	/* global createTagForm */
	"use strict";

	/*
	 * Generates the #courseTags list from the specified tags array.
	 */
	function buildTagList(courseId, tags) {
		// template for tag list items
		var tagListItemTemplate = $("#tagListItemTemplate").html();
		Mustache.parse(tagListItemTemplate);

		// template for rename dialog
		var renameTagDialogTemplate = $("#renameTagDialogTemplate").html();
		Mustache.parse(renameTagDialogTemplate);

		// empty list
		var $tagList = $("ul#courseTags");
		$tagList.empty();

		// insert all tags into the list
		$.each(tags, function(i, tag) {
			// generate HTML code with template
			var rendered = Mustache.render(tagListItemTemplate, {
				"tag" : tag,
				"courseId" : courseId
			});

			var $tagListItem = $(rendered);

			// enable "remove tag from course" Link
			$tagListItem.find(".removeTagLink").click(function() {
				var yes = confirm(messages.reallyRemoveTag);

				if (yes)
					deleteTag(tag.id);
			});

			$tagListItem.find(".changeInstructorOnlyLink").click(function() {
				$.ajax({
					url: window.location.pathname + "/changeInstructorOnly",
					type: "POST",
					data: {
						tagId : tag.id,
						instructorOnly : !tag.instructorOnly
					},
					success: function(response) {
						utils.showOkMessage(response.message);
						reloadTagList();
					}
				});
			});

			// enable "rename" link
			$tagListItem.find(".renameTagLink").click(function() {
				var renameTagDialog = Mustache.render(renameTagDialogTemplate, {
					"tag" : tag
				});
				var $renameTagDialog = $(renameTagDialog);

				$renameTagDialog.find(".okButton").click(function() {
					  var newName = $renameTagDialog.find("#name").val();

					  $.ajax({
						  url: window.location.pathname + "/renameTag",
						  type: "POST",
						  data: {
							  "tagId" : tag.id,
							  "name" : newName
						  },

						  success: function() {
							  reloadTagList();
							  $renameTagDialog.modal("hide");
							  utils.showOkMessage(messages.tagRenamed);
						  }
					  });
				 });

				 $renameTagDialog.find(".cancelButton").click(function() {
					 $renameTagDialog.modal("hide");
				 });

				 $renameTagDialog.modal();
			});

			$tagList.append($tagListItem);
		});
	}

	/*
	 * delete the tag.
	 */
	function deleteTag(tagId) {
		$.ajax({
			url: window.location.pathname + "/deleteTag",
			data: {
				tagId : tagId
			},
			type: "POST",

			success: function() {
				// remove tag from left list
				var $tagLi = $("ul#courseTags > li[data-tag-id=" + tagId + "]");

				$tagLi.slideUp("normal", function() {
					$(this).remove();
					updateAlreadyExisting(); // update right list (in case the tag existed in this one)
				});

				utils.showOkMessage(messages.tagRemoved);
			}
		});
	}

	/*
	 * Creates a new tag for a course.
	 */
	function createTag(tag) {
		createTagForm.createTag(tag)
			.done(function() {
				// Reload the whole list (sorting in the new tag is not worth the effort)
				reloadTagList();

				// empty input field
				$("#newTagName").val("");

				utils.showOkMessage(messages.tagCreated);
			});
	}

	/*
	 * reload the tag list for the course.
	 */
	function reloadTagList() {
		$.ajax({
			url: window.location.pathname + "/getTags",
			type: "GET",

			success: function(tags) {
				buildTagList(courseId, tags);
				updateAlreadyExisting();
			}
		});
	}

	function updateAlreadyExisting() {
		var hideAlreadyExisting = $("#hideExistingCheckbox").prop("checked");
		var $courseTags = $("ul#courseTags > li");
		var $otherTags = $("ul.otherTags > li");

		/* Iterate all tags in the right list and check whether it also exists in the left list.
		 * Then show or hide it, depending on whether "hide already existing" is checked. */
		$otherTags.each(function() {
			var $otherTagLi = $(this);
			var nameHash = $otherTagLi.data("tag-name-hash");
			var isAlreadyExisting = $courseTags.filter("li[data-tag-name-hash=" + nameHash + "]").length > 0;

			$otherTagLi.toggleClass("alreadyExistingTag", isAlreadyExisting);

			if (isAlreadyExisting && hideAlreadyExisting)
				$otherTagLi.slideUp();
			else
				$otherTagLi.slideDown();
		});
	}

	/*
	 * called on click on "merge tags".
	 * Shows the dialog and submits the form.
	 */
	function mergeTags() {
		var $checkedTags = $(".tagCheckBox:checked");

		if ($checkedTags.length < 2) {
			utils.showErrorMessage(messages.min2Tags);
			return;
		}

		// get selected tag objects
		var tags = [];

		$checkedTags.each(function() {
			var $t = $(this);
			var id = $t.val();
			var name = $t.data("name");
			tags.push({
				"id" : id,
				"name" : name
			});
		});

		// create dialog from template
		var mergeTagsDialogTemplate = $("#mergeTagsDialogTemplate").html();
		Mustache.parse(mergeTagsDialogTemplate);

		var renderedDialog = Mustache.render(mergeTagsDialogTemplate, {
			"tags" : tags
		});
		var $renderedDialog = $(renderedDialog);

		// check first tag
		$renderedDialog.find(":radio:first").prop("checked", true);

		$renderedDialog.find(".okButton").click(function() {
			var $form = $renderedDialog.find("form");

			$.ajax({
				url: window.location.pathname + "/mergeTags",
				type: "POST",
				data: $form.serialize(),

				success: function() {
					reloadTagList();
					$renderedDialog.modal("hide");
					utils.showOkMessage(messages.tagsMerged);
				}
			});
		});

		$renderedDialog.find(".cancelButton").click(function() {
			$renderedDialog.modal("hide");
		});

		$renderedDialog.modal();
	}

	var courseId = $("#course").data("course-id");

	// add tag to the course on click on "add tag"
	$(".addTagLink").click(function() {
		var tagId = $(this).data("tag-id");

		$.ajax({
			url: window.location.pathname + "/addTagToCourse",
			data: {
				tagId : tagId
			},
			type: "POST"
		})
		.done(function() {
			reloadTagList(courseId);
			utils.showOkMessage(messages.tagCreated);
		});
	});

	// "create new tag" form
	$("#createTagButton").click(function() {
		var name = $("#newTagName").val();
		var instructorOnly = $("#instructorOnly").prop('checked');

		var tag = {
				name : name,
				courseId : courseId,
				instructorOnly : instructorOnly
		};

		createTag(tag);
	});

	// "already existing" checkbox
	$("#hideExistingCheckbox").change(function() {
		updateAlreadyExisting();
	});

	// only show the tags for the selected course
	$("#otherCoursesSelect").change(function() {
		var selectedCourseId = this.value;

		$(".otherCourseTags").each(function() {
			// hide all expect the ones for the specified course
			var $this = $(this);
			var selected = $this.data("course-id") == selectedCourseId;
			$this.toggle(selected);
		});
	});
	$('#otherCoursesSelect').trigger('change');

	// load tags
	reloadTagList();

	// "merge Tags" button
	$("#mergeButton").click(function() {
		mergeTags();
	});
});