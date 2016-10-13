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
package de.whs.poodle.repositories;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import de.whs.poodle.beans.PoodleUser;

@Repository
public class UserRepository {

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private JdbcTemplate jdbc;

	public PoodleUser getById(int id) {
		return em.find(PoodleUser.class, id);
	}

	public PoodleUser getByUsername(String username) {
		try {
			return em.createQuery("FROM PoodleUser WHERE username = LOWER(:username)", PoodleUser.class)
					.setParameter("username", username)
					.getSingleResult();
		} catch(NoResultException e) {
			return null;
		}
	}

	public void setPasswordHash(int userId, String newPasswordHash) {
		jdbc.update("UPDATE poodle_user SET password_hash = ? WHERE id = ?", newPasswordHash, userId);
	}

	public List<String> getAllEmailRecipients() {
		return jdbc.queryForList(
				"SELECT username FROM poodle_user " +
				"WHERE poodle_user.password_hash IS NULL AND poodle_user.id " +
				"NOT IN (SELECT student.id FROM student " +
						"WHERE student.fake_for_instructor_id IS NOT NULL OR NOT student.cfg_email_messages)",
				String.class);
	}
}