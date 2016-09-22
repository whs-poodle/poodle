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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import de.whs.poodle.services.WorksheetUnlockEmailService;

/*
 * This Scheduler takes care of automatically unlocking worksheets
 * that have an "unlock at" date set by the instructor. It checks every
 * RATE_MS milliseconds for worksheets to be unlocked and unlocks them.
 *
 * This may cause a slight delay between the specified unlock time
 * and the actual unlocking, but this is negligible. In order to guarantee
 * the unlocking at the exact specified time, we would have to create
 * a timer for each worksheet. However, this would be complicated and error prone
 * since you have to keep track of the timers, make sure they are always in sync with the
 * database and restore them on server restart.
 */
@Component
public class WorksheetAutoUnlockScheduler {

	private static final long RATE_MS = 5 * 60 * 1000; // 5 minutes

	private Logger log = LoggerFactory.getLogger(WorksheetAutoUnlockScheduler.class);

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private WorksheetUnlockEmailService worksheetUnlockEmailService;

	// this is automatically called by Spring every RATE_MS milliseconds
	@Scheduled(fixedRate = RATE_MS)
	public void unlockWorksheets() {
		log.info("checking for worksheets to unlock...");

		/* query all locked worksheets that have unlock_at in the past, i.e.
		 * all worksheets that need to be unlocked right now. */
		jdbc.query(
				"SELECT id,unlock_at FROM worksheet " +
				"WHERE NOT unlocked " +
				"AND unlock_at IS NOT NULL " +
				"AND unlock_at <= NOW()",

				new RowCallbackHandler() {

					@Override
					public void processRow(ResultSet rs) throws SQLException {
						int id = rs.getInt("id");
						Date unlockAt = rs.getTimestamp("unlock_at");

						log.info("unlocking worksheet {} with unlock_at = {}", id, unlockAt);

						try {
							worksheetUnlockEmailService.unlockWorksheetAndSendEmail(id);
						} catch (MessagingException e) {
							log.error("failed to send email", e);
						}
					}
				});
	}
}
