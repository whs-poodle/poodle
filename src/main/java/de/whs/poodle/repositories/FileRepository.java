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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import de.whs.poodle.repositories.exceptions.NotFoundException;

@Repository
public class FileRepository {

	@Autowired
	private JdbcTemplate jdbc;

	public int uploadFile(MultipartFile file) throws IOException {
		InputStream in = file.getInputStream();

		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbc.update(
			con -> {
				PreparedStatement ps = con.prepareStatement
						("INSERT INTO uploaded_file(data,mimetype,filename) VALUES(?,?,?)",
						new String[]{"id"});

				ps.setBinaryStream(1, in, file.getSize());
				ps.setString(2, file.getContentType());
				ps.setString(3, file.getOriginalFilename());
				return ps;
			},
			keyHolder);

		return keyHolder.getKey().intValue();
	}


	public void writeFileToHttpResponse(int fileId, HttpServletResponse response) {
		jdbc.query(
			"SELECT filename,mimetype,data FROM uploaded_file WHERE id = ?",
			new Object[]{fileId},

			// use ResultSetExtractor, so we can check whether the row even exists (NotFoundException)
			new ResultSetExtractor<Void>() {

				@Override
				public Void extractData(ResultSet rs) throws SQLException {
					if (!rs.next())
						throw new NotFoundException();

					String filename = rs.getString("filename");
					String mimeType = rs.getString("mimetype");

					response.setHeader("Content-Disposition", "filename=\"" + filename + "\"");
					response.setContentType(mimeType);

					try (
						InputStream in = rs.getBinaryStream("data");
						OutputStream out = response.getOutputStream();
					) {
						StreamUtils.copy(in, out);
						response.flushBuffer();
					} catch(IOException e) {
						throw new RuntimeException(e);
					}

					return null;
				}
			});
	}
}
