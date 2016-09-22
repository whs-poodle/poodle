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
package de.whs.poodle.controllers.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.MultipartProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.whs.poodle.PoodleProperties;
import de.whs.poodle.Utils;
import de.whs.poodle.beans.Exercise;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.Tag;
import de.whs.poodle.beans.statistics.Statistic;
import de.whs.poodle.beans.statistics.StatisticList;
import de.whs.poodle.repositories.ExerciseRepository;
import de.whs.poodle.repositories.FileRepository;
import de.whs.poodle.repositories.McQuestionRepository;
import de.whs.poodle.repositories.StatisticsRepository;
import de.whs.poodle.repositories.TagRepository;
import de.whs.poodle.repositories.exceptions.BadRequestException;
import de.whs.poodle.services.EmailService;

/*
 * Instructor Rest functions that are used in multiple controllers.
 * All Rest functions that are only used on a a single page are located
 * in the corresponding controller.
 */
@RestController
@RequestMapping("/instructor/rest")
public class InstructorRestController {

	private static final Logger log = LoggerFactory.getLogger(InstructorRestController.class);

	@Autowired
	private ExerciseRepository exerciseRepo;

	@Autowired
	private McQuestionRepository mcQuestionRepo;

	@Autowired
	private TagRepository tagRepo;

	@Autowired
	private StatisticsRepository statisticsRepo;

	@Autowired(required = false)
	private EmailService emailService;

	@Autowired
	private PoodleProperties poodle;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private Utils utils;

	@Autowired
	private MultipartProperties multipartProperties;

	@Autowired
	private FileRepository fileRepo;

	// Comment on a feedback/statistic and send the e-mail
	@RequestMapping(value = "commentOnFeedback", method = RequestMethod.POST)
	public Map<String,Object> commentOnFeedback(
			@ModelAttribute Instructor instructor,
			@RequestParam int statisticId,
			@RequestParam String comment) {

		comment = Utils.sanitizeHTML(comment);

		if (comment.trim().isEmpty())
			throw new BadRequestException("commentIsEmpty");

		boolean updated = statisticsRepo.commentOnFeedback(statisticId, instructor.getId(), comment);

		/* The instructor answer was already set (maybe because the instructor
		 * was impatient and clicked the button multiple times). Abort here to avoid
		 * sending the email a second time. */
		if (!updated) {
			log.debug("comment was already set, statistic id = {}", statisticId);
			return utils.simpleMessage("commentSent");
		}

		// no email support, abort
		if (emailService == null) {
			return utils.simpleMessage("commentSent");
		}

		Statistic statistic = statisticsRepo.getById(statisticId);
		Student student = statistic.getStudent();
		Exercise exercise = statistic.getExercise();

		Locale locale = poodle.getServerLocale();

		String subject = messageSource.getMessage(
				"email.feedbackCommented.subject",
				new Object[]{ exercise.getTitle()}, locale);

		String text = messageSource.getMessage(
				"email.feedbackCommented.text",
				new Object[]{instructor.getFullName(), exercise.getTitle(), poodle.getBaseUrl(), exercise.getRootId()},
				locale);

		try {
			emailService.sendMail(instructor, student.getUsername(), null, false, subject, text, false);
			return utils.simpleMessage("commentSentAndSaved");
		} catch(MailException|MessagingException e) {
			log.error("failed to send email", e);
			throw new BadRequestException("feedbackCommentEmailFailed", new Object[]{e.getMessage()});
		}
	}

	/*
	 * setIgnoreStatistic() sets a statistic to "ignore" and returns the new average
	 * values for time, fun and difficulty. The new average values we have
	 * to return depend on the context in which the statistic is ignored.
	 * For example, in the table in feedbackOverview.html we only display
	 * the statistics for the worksheets in this course term, so the average
	 * values should only take those into account.
	 * However, when we display a single exercise (/instructor/exercises/n) we
	 * show all statistics for this exercise, regardless of the course term.
	 * Depending on which page the statistic has been set to ignored, we therefore
	 * have to return different average values.
	 */
	public enum ExerciseAvgValueContext {
		ALL, WORKSHEET, NONE
	};

	@RequestMapping(value = "statistics/{id}/ignore", method = RequestMethod.POST)
	public Map<String,Object> setIgnoreStatistic(
			@PathVariable int id,
			@RequestParam boolean ignore,
			@RequestParam ExerciseAvgValueContext avgValueContext,
			Locale locale) {
		statisticsRepo.setIgnoreStatistic(id, ignore);

		HashMap<String,Object> map = new HashMap<>();

		// add the message that is shown to the user
		String messageCode = ignore ? "feedbackIgnored" : "feedbackNotIgnored";
		String message = messageSource.getMessage(messageCode, null, locale);

		map.put("message", message);

		Statistic statistic = statisticsRepo.getById(id);

		// client may need this
		map.put("exerciseRootId", statistic.getExerciseRootId());

		/* Return the new average values since it probably changed and the client may have to update it.
		 * See comment above function regarding the avgValueContext. */
		StatisticList exerciseStats;

		switch (avgValueContext) {
		case ALL: // return avg for all statistics for this exercise
			exerciseStats = statisticsRepo.getForExerciseRoot(statistic.getExerciseRootId());
			break;

		case WORKSHEET: // return avg for the statistics on the worksheets of the statistics' course term
			exerciseStats = statisticsRepo.getForExerciseOnWorksheet(
					statistic.getExerciseRootId(), statistic.getCourseTerm().getId());
			break;

		case NONE:
		default:
			exerciseStats = null;
			break;
		}

		if (exerciseStats != null) {
			map.put("avgDifficultyStr", exerciseStats.getAvgDifficultyStr());
			map.put("avgTimeStr", exerciseStats.getAvgTimeStr());
			map.put("avgFunStr", exerciseStats.getAvgFunStr());
		}

		return map;
	}

	@RequestMapping(value = "exercises/{exerciseId}", method = RequestMethod.DELETE)
	public void deleteExercise(@PathVariable int exerciseId) {
		exerciseRepo.delete(exerciseId);
	}

	@RequestMapping(value = "mcQuestions/{mcQuestionId}", method = RequestMethod.DELETE)
	public void deleteMcQuestion(@PathVariable int mcQuestionId) {
		mcQuestionRepo.delete(mcQuestionId);
	}

	@RequestMapping(value = "tags", method = RequestMethod.POST)
	public Tag createTag(@ModelAttribute Tag tag) {
		return tagRepo.createTag(tag);
	}

	@RequestMapping(value = "files", method = RequestMethod.POST)
	public Map<String,Object> uploadFile(@RequestParam MultipartFile file) throws IOException {
		int fileId = fileRepo.uploadFile(file);
		return Collections.singletonMap("id", fileId);
	}

	// handle MultipartException (s. Poodle.java)
	@RequestMapping("uploadError")
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String,Object> onUploadError() {
		Locale locale = LocaleContextHolder.getLocale();
		String maxFileSize = multipartProperties.getMaxFileSize();

		String message = messageSource.getMessage("fileUploadSizeLimitExceeded", new Object[]{ maxFileSize }, locale);
		return Collections.singletonMap("message", message);
	}
}
