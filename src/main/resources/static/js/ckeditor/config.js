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
	"use strict";

	// load our "link exercise" plugin
	CKEDITOR.plugins.addExternal('exerciseLink', utils.contextPath + 'js/ckeditor/exerciseLinkPlugin/plugin.js', '');
})();

/* exported ckeditorConfig */
var ckeditorConfig = (function() {
	/* global messages */
	"use strict";

	/* We need the course ID for the config since images are saved per
	 * course and we therefore the ID to browse/upload images.
	 * customConfig is config specific to the editor (height etc.)
	 * which will simply be added. */
	function createConfig(courseId, customConfig) {
		var cfg = {};

		cfg.language = messages.CKEditorLanguage;
		cfg.courseId = courseId;

		// URL that CKEditor uses to upload images
		cfg.filebrowserImageUploadUrl = utils.contextPath + 'instructor/images/' + courseId;

		// image browser URL
		cfg.filebrowserBrowseUrl = utils.contextPath + 'instructor/browseImages?courseId=' + courseId;

		// use MathJax URL from our existing <script> tag
		cfg.mathJaxLib = $('#mathJaxScript').attr('src');
		cfg.extraPlugins = 'mathjax,exerciseLink';

		// load our custom styles
		var stylesUrl = utils.contextPath + "js/ckeditor/styles.js";
		cfg.stylesSet = "default:" + stylesUrl;

		// add customConfig, if any
		if (customConfig)
			$.extend(true, cfg, customConfig);

		return cfg;
	}

	return {
		create : createConfig
	};
})();