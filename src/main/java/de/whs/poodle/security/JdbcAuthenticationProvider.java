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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/*
 * Configures the JDBC-based authentication.
 *
 * @Ordered makes sure, that this Provider is the first
 * in the authList in SpringSecurityConfig and therefore
 * the first to be tried for authentication. This is useful
 * in case the LDAP server is down but an admin still wants
 * to log in via JDBC.
 */
@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class JdbcAuthenticationProvider implements PoodleAuthenticationProvider {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.jdbcAuthentication()
			.dataSource(dataSource)
			/* The passwords are stores as BCrypt hashes in the database, created by the
			 * password encoder. We only have to tell Spring how to get the username and the hash
			 * and it will do the password checking by itself. We have to check for "password_hash IS NOT NULL"
			 * here since we have to ignore LDAP users. Otherwise Spring throws an internal Exception
			 * due to the NULL password and does not continue trying the LDAP authentication. */
			.passwordEncoder(passwordEncoder)
			.usersByUsernameQuery("SELECT username,password_hash,TRUE FROM poodle_user WHERE username = LOWER(?) AND password_hash IS NOT NULL")
			/* Query to determine the roles for a user. Spring expects a table for this. We don't have that and
			 * simply use a function to determine the roles instead. */
			.authoritiesByUsernameQuery("SELECT username,role FROM get_user_roles(LOWER(?))")
			.rolePrefix("ROLE_");
	}
}
