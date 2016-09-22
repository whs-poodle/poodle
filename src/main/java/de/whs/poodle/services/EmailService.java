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

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.OrFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import de.whs.poodle.PoodleProperties;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.repositories.exceptions.NotFoundException;

/*
 * Service class to send emails.
 *
 * The JavaMailSender is created by Spring Boots MailSenderAutoConfiguration,
 * s. http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-email
 */
@Service
@ConditionalOnProperty("poodle.emailEnabled")
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	private LdapTemplate ldap;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private PoodleProperties poodle;

	@Autowired
	public EmailService(ContextSource ldapContextSource) {
		this.ldap = new LdapTemplate(ldapContextSource);
	}

	/*
	 * Sends an email from an instructor to one specified username (toUsername)
	 * or a list of usernames as Bcc (bccUsernames) or both.
	 * senderBcc specifies whether the sender should be added to the bcc list e.g.
	 * wants to have copy of the mail.
	 * If setNoReply is set, we set the no-reply address as specified in the application.properties
	 * as the "reply to" address.
	 */
	public void sendMail(Instructor from, String toUsername, List<String> bccUsernames, boolean senderBcc, String subject, String text, boolean setNoReply) throws MessagingException {
		if (bccUsernames == null)
			bccUsernames = new ArrayList<>();

		if (toUsername == null && bccUsernames.isEmpty()) {
			log.info("sendMail(): no recipients, aborting.");
			return;
		}

		// may be empty if bccUsernames was not specified
		List<String> bccEmails = getEmailsForUsernames(bccUsernames);

		String fromEmail = getEmailForUsername(from.getUsername());

		// create "from" as "FirstName LastName <email@w-hs.de>"
		String fromStr = String.format("%s %s <%s>", from.getFirstName(), from.getLastName(), fromEmail);

		if (senderBcc)
			bccEmails.add(fromEmail);

		// create MimeMessage
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

		helper.setSubject("[Poodle] " + subject);
		helper.setFrom(fromStr);

		if (toUsername != null) {
			String toEmail = getEmailForUsername(toUsername);
			helper.setTo(toEmail);
		}

		if (!bccEmails.isEmpty())
			helper.setBcc(bccEmails.toArray(new String[0]));

		if (setNoReply)
			helper.setReplyTo(poodle.getEmailNoReplyAddress());

		helper.setText(text);

		mailSender.send(mimeMessage);
	}

	private String getEmailForUsername(String username) {
		LdapQuery query = LdapQueryBuilder.query().where("cn").is(username);

		List<String> mails = ldap.search(query, new MailAttributeMapper());

		if (mails.isEmpty()) // no email for this user found
			throw new NotFoundException();
		else
			return mails.get(0);
	}

	private List<String> getEmailsForUsernames(List<String> usernames) {
		if (usernames.isEmpty())
			return new ArrayList<>();

		OrFilter filter = new OrFilter();
		for (String l : usernames)
			filter.or(new EqualsFilter("cn", l));

		LdapQuery query = LdapQueryBuilder.query().filter(filter);

		return ldap.search(query, new MailAttributeMapper());
	}

	private static class MailAttributeMapper implements AttributesMapper<String> {

		@Override
		public String mapFromAttributes(Attributes attributes) throws NamingException {
			return attributes.get("mail").get().toString();
		}

	}
}
