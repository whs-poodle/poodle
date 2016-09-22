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
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StreamUtils;

import de.whs.poodle.beans.UploadedImage;
import de.whs.poodle.repositories.exceptions.NotFoundException;

@Repository
public class ImageRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@PersistenceContext
	private EntityManager em;

	/*
	 * write the image into the database and return the generated object.
	 */
	public void uploadImage(UploadedImage image, InputStream in, long length) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbc.update(
			con -> {
				PreparedStatement ps = con.prepareStatement
						("INSERT INTO uploaded_image(filename,mimetype,data,instructor_id,course_id) VALUES(?,?,?,?,?)",
						new String[]{"id"});

				ps.setString(1, image.getFilename());
				ps.setString(2, image.getMimeType());
				ps.setBinaryStream(3, in, length);
				ps.setInt(4, image.getInstructor().getId());
				ps.setInt(5, image.getCourseId());
				return ps;
			},
			keyHolder);

		int id = keyHolder.getKey().intValue();
		image.setId(id);
	}

	public void writeImageToHttpResponse(int imageId, HttpServletResponse response) {
		jdbc.query(
			"SELECT mimetype,data FROM uploaded_image WHERE id = ?",
			new Object[]{imageId},

			// use ResultSetExtractor so we can check whether a row even existed (NotFoundException)
			new ResultSetExtractor<Void>() {

				@Override
				public Void extractData(ResultSet rs) throws SQLException {
					if (!rs.next()) // image doesn't exist
						throw new NotFoundException();

					String mimeType = rs.getString("mimetype");
					response.setContentType(mimeType);

					// write input stream from DB into http response
					try (
						InputStream in = rs.getBinaryStream("data");
						OutputStream out = response.getOutputStream();
					) {
						StreamUtils.copy(in, out);
						response.flushBuffer();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

					return null;
				}

			});
	}

	public List<UploadedImage> getForCourse(int courseId) {
		return em.createQuery("FROM UploadedImage WHERE courseId = :courseId ORDER BY uploadedAt DESC", UploadedImage.class)
				.setParameter("courseId", courseId)
				.getResultList();
	}
}
