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
	"use strict";

	/* format function we use to substitute parameters in our
	 * i18n messages, e.g. "Hello {0}".format("test") -> "Hello test" */
	String.prototype.format = function() {
		var args = arguments;
		return this.replace(/{(\d+)}/g, function(match, number) {
			return typeof args[number] != 'undefined' ? args[number] : match;
		});
	};
}());

// general functions that are used in the site-specific JS-files.
var utils = (function() {
	/* global messages */
	"use strict";

	/* Duration in ms, until an ok/error message disappears (s. showMessage()). */
	var OK_MESSAGE_DURATION = 3000;
	var ERROR_MESSAGE_DURATION = 6000;

	// make the contextPath available (for ajax URLs etc.)
	var contextPath = $("#globaljs").data("context-path");

	return {
		contextPath : contextPath,

		showOkMessage: function(message) {
			utils.showMessage(message, false);
		},

		showErrorMessage: function(message) {
			utils.showMessage(message, true);
		},

		// show on OK/Error message in the bottom left corner
		showMessage: function(message, isError) {
			var ajaxMessageClass = isError ? "alert-danger" : "alert-success";
			var delay = isError ? ERROR_MESSAGE_DURATION : OK_MESSAGE_DURATION;

			var $messageLi = $("<li>")
							.addClass("alert")
							.addClass(ajaxMessageClass)
							.prop("title", messages.close)
							.css("display", "none")
							.append(message);

			// remove on click
			$messageLi.click(function() {
				$(this).remove();
			});

			// add to list and show
			$("#ajaxMessages").append($messageLi);
			$messageLi.slideDown();

			// hide after X seconds and remove from list
			$messageLi.delay(delay).slideUp(function() {
				$(this).remove();
			});
		},

		// upload given file and return ajax call
		uploadFile: function(file) {
			var formData = new FormData();
			formData.append("file", file);

			return $.ajax({
				url: contextPath + "instructor/rest/files",
				type: "POST",
				contentType: false,
				processData: false,
				data: formData
			});
		},

		redirect: function(path) {
			window.location.replace(contextPath + path);
		}
	};
})();

$(document).ready(function() {
	"use strict";

	function initAjax() {

		function defaultAjaxErrorHandler(event, xhr, ajaxSettings, thrownError) {
			/* session is expired, redirect to login page. */
			if (xhr.status === 401) {
				utils.redirect("?sessionExpired=1");
				return;
			}

			var message;

			/* If the responseText is empty, the server has not responded (Timeout etc.).
			 * The server always returns a JSON object with a "message". */
			if (xhr.responseText === undefined)
				message = messages.ajaxError.format(thrownError);
			else
				message = $.parseJSON(xhr.responseText).message;

			utils.showErrorMessage(message);
		}

		// global ajax settings
		$.ajaxSetup({
			timeout: 30000
		});

		// show the loading indicator during every ajax request
		$(document)
		.ajaxSend(function(event, xhr, options) {
			if (!options.noLoadingIndicator)
				$("#ajaxLoadingIndicator").show();
		})
		.ajaxStop(function() {
			$("#ajaxLoadingIndicator").hide();
		})
		// set default error handler
		.ajaxError(defaultAjaxErrorHandler);
	}

	// Bootstrap Popovers
	function initPopovers() {
		var $popovers = $("[data-toggle='popover']");

		// if the element has data-content, we show a simple popover with plain text
		$popovers.filter("[data-content]").popover();

		/* HTML Popovers. If a popover has a data-content-id instead of data-content,
		 * the content of the element referenced by the content-id will be shown
		 * as an HTML popover. */
		$popovers.filter("[data-content-id]").popover({
			html: true,
			content: function() {
				var contentId = $(this).data("content-id");
				return $("#" + contentId).html();
			}
		});

		/* When a popover is opened, close all other ones
		 * to make sure that only one is ever open. */
		$("body").on("show.bs.popover", function(e) {
			// not using $popovers here so we also match dynamically added popovers
			$("[data-toggle='popover']").not(e.target).popover("hide");
		});
	}

	// Bootstrap Tooltips
	function initTooltips() {
		$("[data-toggle='tooltip']").bootstrapTooltip({
			'container':'body',
			delay : {
				show : 500,
				hide : 0
			}
		});
	}

	/* For every element that has a data-toggle-id attribute,
	 * show/hide the corresponding element. We use this e.g. to show
	 * the feedback form when a student clicks on the "give feedback" link. */
	function initToggleId() {
		$("[data-toggle-id]").click(function() {
			var $button = $(this);
			var id = $button.data("toggle-id");

			$("#" + id).slideToggle(function() {
				if ($(this).is(":visible")) {
					// optionally focus the element specified by "data-focus-selector"
					var focusSelector = $button.data("focus-selector");
					if (focusSelector)
						$(focusSelector).focus();
				}
			});
		});
	}

	/* Since our nav is fixed, we have to add a padding-top to the body
	 * so that the nav doesn't overlap the body. We have to set this
	 * padding on every window resize since in a worst-case-scenario
	 * the nav can become two rows high. */
	function initBodyPadding() {
		var $nav = $("nav");
		if ($nav.is(":hidden"))
			return;

		function recalcBodyPadding() {
			var navHeight = $nav.height();
			$("body").css("padding-top", (navHeight + 10) + "px");
		}

		recalcBodyPadding();

		$(window).resize(recalcBodyPadding);
	}

	// hide nav, header etc. if we are in an iframe
	if (window != window.top)
		$(document.body).addClass("iframe");

	initAjax();
	initPopovers();
	initTooltips();
	initToggleId();
	initBodyPadding();
});