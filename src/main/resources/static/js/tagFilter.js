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

	function setupTagFilter() {
		var $filterInput = $("#tagFilter input");
		var timer = false;

		var onTagFilterKeyDown = function() {

			if (timer)
				clearTimeout(timer);

			// Update the "matched" tags every 100 ms.
			timer = setTimeout(function() {
				var filter = $filterInput.get(0).value.toLowerCase();
				var $labels = $("#tagFilter label");

				$labels.each(function() {
					if (filter.length > 0) {
						if (this.textContent.toLowerCase().indexOf(filter) != -1)
							$(this).addClass('matched');
						else
							$(this).removeClass('matched');
					}
					else {
						$(this).removeClass('matched');
					}
				});
			}, 100);
		};

		$filterInput.keydown(onTagFilterKeyDown);
	}

	setupTagFilter();
});