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
	"use strict";

	// when an image is clicked return the image to the CKEditor instance
	$('.chooseImageLink').click(function() {
		var CKEditorFuncNum = $(this).data('ckeditor');
		var imagePath = $(this).data('image-path');
		window.opener.CKEDITOR.tools.callFunction(CKEditorFuncNum, imagePath);
		window.close();
	});

	// submit form on change
	$("#filterForm select").change(function() {
		$("#filterForm").submit();
	});
});