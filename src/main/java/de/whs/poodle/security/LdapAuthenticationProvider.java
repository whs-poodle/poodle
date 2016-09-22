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
package de.whs.poodle.security;

import java.util.ArrayList;
import java.util.Collection;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.stereotype.Component;

import de.whs.poodle.LdapLoginProperties;
import de.whs.poodle.beans.Instructor;
import de.whs.poodle.repositories.InstructorRepository;
import de.whs.poodle.repositories.StudentRepository;

/*
 * Logic for the LDAP based authentication.
 *
 * LDAP docs
 * http://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#ldap-server
 */
@Component
// only enable this if the LdapLoginProperties have been set
@ConditionalOnBean(LdapLoginProperties.class)
public class LdapAuthenticationProvider implements PoodleAuthenticationProvider {

	private static final Logger log = LoggerFactory.getLogger(LdapAuthenticationProvider.class);

	@Autowired
	private LdapLoginProperties ldapProperties;

	@Autowired
	private BaseLdapPathContextSource ldapContextSource;

	@Autowired
	private InstructorRepository instructorRepo;

	@Autowired
	private StudentRepository studentRepo;

	@Override
	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.ldapAuthentication()
			.contextSource(this.ldapContextSource)
			.ldapAuthoritiesPopulator(new PoodleLdapAuthoritiesPopulator())
			.userSearchFilter(ldapProperties.getUserSearchFilter());
	}

	 private class PoodleLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {

		/*
		 * This is called after a successful login to determine the roles for the user.
		 * we check the LDAP "memberOf" attribute to decide whether the user is an
		 * instructor or a student. The ADMIN role is only given to the user if
		 * this is set in our database.
		 */
		@Override
		public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations ctx, String username) {
			log.debug("determining roles for {}", username);

			ArrayList<GrantedAuthority> authorities = new ArrayList<>();
			Attributes attributes = ctx.getAttributes();

			try {
				Attribute memberOf = attributes.get("memberOf");
				if (memberOf == null) {
					log.warn("user {} has no memberOf attribute, can't check groups", username);
					return authorities; // empty at this point
				}

				NamingEnumeration<?> groups = memberOf.getAll();
				log.debug("reading groups for {} via LDAP", username);

				while (groups.hasMore()) {
					String group = (String)groups.next();
					log.debug("User {} is in group {}", username, group);

					if (group.equalsIgnoreCase(ldapProperties.getStudentGroup())) {
						log.debug("User {} is a student", username);
						authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));

						studentRepo.createIfNotExistsAndGet(username);
						break;
					}
					else if (group.equalsIgnoreCase(ldapProperties.getInstructorGroup())) {
						log.debug("User {} ist an instructor, reading name via LDAP and saving it to our database", username);
						authorities.add(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"));
						/* Get first and last name and store it in our database. */
						String lastName = attributes.get("sn").get().toString();
						String firstName = attributes.get("givenName").get().toString();

						Instructor instructor = new Instructor();
						instructor.setFirstName(firstName);
						instructor.setLastName(lastName);
						instructor.setUsername(username);

						log.debug("instructor {}, last name: {}, first name: {}. writing to database", username, lastName, firstName);

						instructor = instructorRepo.createOrUpdate(instructor);
						if (instructor.isAdmin()) {
							log.debug("Instructor {} is marked as admin in database", username);
							authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
						}

						break;
					}
				}
			} catch (NamingException e) {
				throw new RuntimeException(e);
			}

			if (authorities.isEmpty())
				log.error("according to the LDAP groups, user {} is neither instructor nor student", username);

			return authorities;
		}
	}
}