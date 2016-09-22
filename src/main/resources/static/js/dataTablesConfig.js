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
/*
 * Loads config and customization for DataTables.
 * This file only loaded if DataTables is actually needed
 * on the page (see layout.html).
 */
$(document).ready(function() {
	/* global messages */
	"use strict";

	/* Functions to automatically scroll to the top if one
	 * of the paginate buttons is clicked (see paginateScroll()).*/
	function reinitPaginateScroll() {
		/* We have to reinit the events every time since DataTables
		 * regenerates the buttons occasionally. */
		var $paginateButtons = $(".paginate_button");
		$paginateButtons.unbind('click', paginateScroll);
		$paginateButtons.bind('click', paginateScroll);
	}

	function paginateScroll() {
		/* Since our navigation is fixed, we have to subtract the padding
		 * of the body from the table position. Otherwise the navigation
		 * overlaps the table. */
		var bodyPaddingTop = parseInt($("body").css("padding-top"));
		var scrollTopPos = $(".dataTables_wrapper").offset().top - bodyPaddingTop;

		// scroll to top
		$("html, body").animate({
			scrollTop: scrollTopPos
		}, 250);

		reinitPaginateScroll();
	}

	// default settings
	$.extend($.fn.dataTable.defaults, {
		"fnDrawCallback": reinitPaginateScroll,
		"bFilter" : false
	});

	// load language specified by our current locale
	if (messages.dataTablesLanguageUrl !== undefined) {
		$.fn.dataTable.defaults.language = {
			url : messages.dataTablesLanguageUrl
		};
	}
});