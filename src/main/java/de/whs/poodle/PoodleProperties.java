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
package de.whs.poodle;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * Bean for various Poodle settings. The attributes are filled
 * by Spring based on the poodle.* properties in application.properties.
 */
@Component
@ConfigurationProperties(prefix = "poodle")
public class PoodleProperties {

	/* Whether email support should be enabled.
	 * When this is set to "true", we assume that
	 * the JavaMailSender (Spring Boot's MailSenderAutoConfiguration)
	 * and BaseLdapPathContextSource (Poodle.java) beans exist
	 * since we need both to send emails.*/
	private boolean emailEnabled;

	// reply address to be set when sending emails
	private String emailNoReplyAddress;

	// base URL of the website. We need this to generate links in emails.
	private String baseUrl;

	// locale used to send emails
	private Locale serverLocale = Locale.ENGLISH;

	private String forgotPasswordLink;

	private String reportBugLink;

	// header logo on login page
	private Logo logo = new Logo();

	private String faviconFilename = "whs-favicon.png";

	public boolean isEmailEnabled() {
		return emailEnabled;
	}

	public void setEmailEnabled(boolean emailEnabled) {
		this.emailEnabled = emailEnabled;
	}

	public String getEmailNoReplyAddress() {
		return emailNoReplyAddress;
	}

	public void setEmailNoReplyAddress(String emailNoReplyAddress) {
		this.emailNoReplyAddress = emailNoReplyAddress;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public Locale getServerLocale() {
		return serverLocale;
	}

	public void setServerLocale(Locale serverLocale) {
		this.serverLocale = serverLocale;
	}

	public void setBaseUrl(String baseUrl) {
		// remove trailing slash, if there is one.
		if(baseUrl.endsWith("/"))
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

		this.baseUrl = baseUrl;
	}

	public String getForgotPasswordLink() {
		return forgotPasswordLink;
	}

	public void setForgotPasswordLink(String forgotPasswordLink) {
		this.forgotPasswordLink = forgotPasswordLink;
	}

	public String getReportBugLink() {
		return reportBugLink;
	}

	public void setReportBugLink(String reportBugLink) {
		this.reportBugLink = reportBugLink;
	}

	public Logo getLogo() {
		return logo;
	}

	public void setLogo(Logo logo) {
		this.logo = logo;
	}

	public String getFaviconFilename() {
		return faviconFilename;
	}

	public void setFaviconFilename(String faviconFilename) {
		this.faviconFilename = faviconFilename;
	}

	public static class Logo {

		private String filename = "whs-logo.png";
		private int width = 275;
		private int height;

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}
	}
}
