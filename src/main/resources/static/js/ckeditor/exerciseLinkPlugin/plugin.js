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
(function() {
	/* global utils */
	/* global messages */
	"use strict";

	CKEDITOR.plugins.add('exerciseLink', {
		icons : "exerciseLink",

		init: function(editor) {
			// define CKEditor command
			editor.addCommand('exerciseLink', {
				exec : onExerciseLinkButtonClick
			});

			// add button
			editor.ui.addButton('exerciseLink', {
				label: messages.linkExercise,
				command: 'exerciseLink',
				toolbar: 'links'
			});
		}
	});

	function onExerciseLinkButtonClick(editor) {
		// get dialog and child iframe
		var $dialog = $("#linkExerciseDialog");
		var $iframe = $dialog.find(".exerciseLinkIframe");

		// off() to remove previously added handler (this is always the same iframe element)
		$iframe.off("load").load(function() {
			/* As soon as the iframe is loaded, add a click handler to the
			 * exerciseLink-Links in the iframe (see exerciseSearch.html). */
			$iframe.contents().find(".exerciseLinkLink").click(function() {
				var $link = $(this);
				var title = $link.data("title");
				var rootId = $link.data("root-id");

				// create link for CKEditor
				var ckLink = editor.document.createElement('a');
				ckLink.setAttribute("href", utils.contextPath + "student/exercises/" + rootId);
				ckLink.setAttribute("title", title);
				ckLink.setText(title);

				editor.insertElement(ckLink);

				$dialog.modal("hide");
			});
		});

		// load the iframe content
		var courseId = editor.config.courseId;
		var iframeSrc = utils.contextPath + "instructor/exerciseSearch?search=1&exerciseLink=1&courses=" + courseId;

		$iframe.attr("src", iframeSrc);

		// show dialog
		$dialog.modal({
			show: true
		});
	}
})();