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

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import de.whs.poodle.cli.PoodleCli;


/*
 * Allows starting Poodle with an embedded Tomcat and configures some necessary Spring Beans.
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
public class Poodle {

	public static void main(String[] args) {
		/* if the first argument is "cli", we run the CLI, otherwise the
		 * web application. */
		boolean runCli = args.length > 0 && args[0].equals("cli");

		if (runCli) {
			// remove first argument ("cli"), the CLI doesn't care about it
			String[] cliArgs = Arrays.copyOfRange(args, 1, args.length);
			PoodleCli.run(cliArgs);
		}
		else {
			// run as normal web app
			SpringApplication.run(Poodle.class, args);
		}
	}

	/* We need LdapContextSource for the login and reading the Email addresses in EmailService.
	 * We only create it if the necessary configuration exists (ConditionalOnBean). */
	@Bean
	@Autowired
	@ConditionalOnBean(LdapLoginProperties.class)
	public BaseLdapPathContextSource ldapContextSource(LdapLoginProperties ldapProperties) {
		DefaultSpringSecurityContextSource ldapContextSource = new DefaultSpringSecurityContextSource(
				ldapProperties.getUrls(), ldapProperties.getBaseDn());
		ldapContextSource.setUserDn(ldapProperties.getUserDn());
		ldapContextSource.setPassword(ldapProperties.getPassword());
		return ldapContextSource;
	}

	/* LocaleResolver that defines the locale for a request
	 * based on a cookie. Is used by LocaleChangeInterceptor
	 * to change the locale. */
	@Bean(name = "localeResolver")
	public LocaleResolver cookieLocaleResolver() {
		return new CookieLocaleResolver();
	}

	@Bean
	public EmbeddedServletContainerCustomizer containerCustomizer() {
		return container -> {
			/*
			 * MultipartExceptions (file upload size limit exceeded etc.) are a
			 * special kind of exception because they are handled by the servlet
			 * container itself before our own code is even executed. In order
			 * to handle this exception properly, we have to register our own
			 * error page with the container. Since all our file uploads happen
			 * via Ajax, this is handled by the InstructorRestController.
			 */
			container.addErrorPages(new ErrorPage(MultipartException.class, "/instructor/rest/uploadError"));
		};
	}

	@Bean
	@Autowired
	public WebMvcConfigurerAdapter webMvcConfigurerAdapter(MessageSource messageSource) {
		return new WebMvcConfigurerAdapter() {

			/* Add LocaleChangeInterceptor which allows us to change the
			 * locale of the LocaleResolver by simply appending a GET parameter
			 * (e.g. lang=de) to the current URL. */
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				LocaleChangeInterceptor l = new LocaleChangeInterceptor();
				l.setParamName("lang");
				registry.addInterceptor(l);
			}

			/* By default, the messages for Java Bean Validation (@Size, @Max etc.) have
			 * to be defined in a separate MessageSource (ValidationMessages.properties).
			 * We define our own validator here so we can set it to use our default
			 * MessageSource and define all message codes in one single file. */
			@Override
			public Validator getValidator() {
				LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
				bean.setValidationMessageSource(messageSource);
				return bean;
			}
		};
	}
}
