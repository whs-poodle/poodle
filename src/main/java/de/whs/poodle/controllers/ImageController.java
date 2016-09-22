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
package de.whs.poodle.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.UploadedImage;
import de.whs.poodle.beans.forms.BrowseImagesFilter;
import de.whs.poodle.repositories.CourseRepository;
import de.whs.poodle.repositories.ImageRepository;

/*
 * Controller for the image integration with CKEditor (Upload and ImageBrowser).
 * Also provides /images/{id} to download an image.
 *
 * See also http://docs.ckeditor.com/#!/guide/dev_file_browser_api regarding the CKEditor API
 */
@Controller
public class ImageController {

	@Autowired
	private ImageRepository imageRepo;

	@Autowired
	private CourseRepository courseRepo;

	protected static Logger log = LoggerFactory.getLogger(ImageController.class);

	// maximum time a client can cache an image
	private static final int IMAGE_CACHE_MAX_AGE = 365 * 24 * 60 * 60; // one year

	/*
	 * The image browser called by CKEditor on click on "browse server".
	 */
	@RequestMapping(value="/instructor/browseImages", method = RequestMethod.GET)
	public String browseImages(Model model, @ModelAttribute Instructor instructor, @ModelAttribute BrowseImagesFilter filter) {
		List<Course> courses = courseRepo.getAllForInstructor(instructor.getId());
		List<UploadedImage> images = imageRepo.getForCourse(filter.getCourseId());
		model.addAttribute("courses", courses);
		model.addAttribute("images", images);
		model.addAttribute("filter", filter);
		return "instructor/browseImages";
	}

	/*
	 * Called by CKEditor to upload an image after it has been chosen by the user. Uploads
	 * the image and returns the path according to the CKEditor File API.
	 */
	@RequestMapping(value="/instructor/images/{courseId}", method = RequestMethod.POST)
	@ResponseBody // send the response directly to the client instead of rendering an HTML page
	public String uploadImage(
			@ModelAttribute Instructor instructor,
			@PathVariable int courseId,
			@RequestParam int CKEditorFuncNum,
			MultipartHttpServletRequest request) throws IOException {
		InputStream in = null;

		try {
			String filename = request.getFileNames().next();
			MultipartFile multipartFile = request.getFile(filename);
			String originalFilename = multipartFile.getOriginalFilename();

			String mimetype = multipartFile.getContentType();
			in = multipartFile.getInputStream();
			long length = multipartFile.getSize();

			UploadedImage image = new UploadedImage();
			image.setCourseId(courseId);
			image.setInstructor(instructor);
			image.setFilename(originalFilename);
			image.setMimeType(mimetype);

			imageRepo.uploadImage(image, in, length);

			String path = request.getContextPath() + "/images/" + image.getId();

			return generateResponse(CKEditorFuncNum, path, null);

		} catch (Exception e) {
			log.error("Error uploading image", e);
			return generateResponse(CKEditorFuncNum, null, "Fehler beim Upload");
		} finally {
			if (in != null)
				in.close();
		}
	}

	/*
	 * Generates the callback for the CKEditor that it needs after a file has been uploaded.
	 */
	private String generateResponse(int CKEditorFuncNum, String path, String message) {
		if (message != null) { // error
			return "<script type='text/javascript'>window.parent.CKEDITOR.tools.callFunction(" + CKEditorFuncNum + ", '', '" + message + "');</script>";
		}
		else {
			return "<script type='text/javascript'>window.parent.CKEDITOR.tools.callFunction(" + CKEditorFuncNum + ", '" + path + "');</script>";
		}
	}

	/*
	 * Allows downloading an image.
	 */
	@RequestMapping(value="/images/{imageId}", method = RequestMethod.GET)
	public void getImage(@PathVariable int imageId, HttpServletResponse response) throws IOException {
		/* Allow the client to cache the image. We can assume that a particular image
		 * doesn't change anymore once it is in the database. */
		response.setHeader("Cache-Control", "max-age=" + IMAGE_CACHE_MAX_AGE);
		response.setHeader("Pragma", "public"); // shouldn't be necessary, but defaults to no-cache, so override it

		imageRepo.writeImageToHttpResponse(imageId, response);
	}
}
