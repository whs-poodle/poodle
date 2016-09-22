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
package de.whs.poodle.services;

import java.util.List;
import java.util.Locale;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.whs.poodle.PoodleProperties;
import de.whs.poodle.beans.Course;
import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.beans.Worksheet;
import de.whs.poodle.repositories.CourseTermRepository;
import de.whs.poodle.repositories.WorksheetRepository;

/*
 * Service to send the "worksheet has been unlocked" email.
 * This is in a separate class since we have to be able to use it with
 * automatic and manual unlocking.
 */
@Service
public class WorksheetUnlockEmailService {

	@Autowired
	private WorksheetRepository worksheetRepo;

	@Autowired
	private CourseTermRepository courseTermRepo;

	@Autowired
	private MessageSource messageSource;

	@Autowired(required = false)
	private EmailService emailService;

	@Autowired
	private PoodleProperties poodle;

	@Transactional
	public void unlockWorksheetAndSendEmail(int worksheetId) throws MessagingException {
		boolean unlocked = worksheetRepo.unlock(worksheetId);

		/* if unlocked is false, this means the UPDATE had no effect i.e.
		 * the worksheet was already unlocked. Abort here to avoid sending
		 * another email. */
		if (!unlocked)
			return;

		// no email support, abort
		if (emailService == null)
			return;

		// send email
		Worksheet worksheet = worksheetRepo.getById(worksheetId);
		CourseTerm courseTerm = worksheet.getCourseTerm();

		Course course = courseTerm.getCourse();
		Instructor instructor = course.getInstructor();

		Locale locale = poodle.getServerLocale();

		List<String> studentUsernames = courseTermRepo.getWorksheetUnlockedEmailRecipients(courseTerm.getId());

		/* Get the correct text and subject, depending on the worksheet type.
		 * Note that the worksheet title is not used in the message codes
		 * for evaluation worksheets, but we pass it here anyway since an if()
		 * for this wouldn't really have any benefit. */
		String subject = messageSource.getMessage(
				 "email.worksheetUnlocked.subject." + worksheet.getType().name(),
				 new Object[] {course.getName(), worksheet.getTitle()},
				 locale);

		String text = messageSource.getMessage(
				 "email.worksheetUnlocked.text." + worksheet.getType().name(),
				 new Object[] {worksheet.getTitle(), course.getName(), poodle.getBaseUrl(), worksheet.getId()},
				 locale);

		emailService.sendMail(instructor, null, studentUsernames, false, subject, text, true);
	 }
}
