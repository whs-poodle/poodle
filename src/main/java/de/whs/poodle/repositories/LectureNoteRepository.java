/*
 * Copyright 2016 Westfälische Hochschule
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
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import de.whs.poodle.beans.LectureNote;

@Repository
public class LectureNoteRepository {

	@Autowired
	private JdbcTemplate jdbc;

	@PersistenceContext
	private EntityManager em;

	public LectureNote getById(int id) {
		return em.find(LectureNote.class, id);
	}

	public List<LectureNote> getForCourse(int courseId) {
		return em.createQuery("FROM LectureNote WHERE course.id = :courseId ORDER BY num ASC", LectureNote.class)
				.setParameter("courseId", courseId)
				.getResultList();
	}

	public List<String> getGroupnames(int courseId) {
		List<String> groupnames = em.createQuery("SELECT groupname FROM LectureNote WHERE course.id = :courseId ORDER BY groupname ASC", String.class)
				.setParameter("courseId", courseId)
				.getResultList();
		return groupnames.stream().distinct().collect(Collectors.toList());
	}

	public int getCourseId(int courseTermId) {
		return jdbc.queryForObject(
			"SELECT course_id FROM course_term " +
			"WHERE id = ?", new Object[] {courseTermId}, Integer.class);
	}

	public int getNodeCountForGroup(String groupname) {
		int nodeCount = em.createQuery(
			"SELECT COUNT(groupname) FROM LectureNote " +
			"WHERE groupname = :groupname", Long.class)
			.setParameter("groupname", groupname)
			.getSingleResult()
			.intValue();
		return nodeCount;
	}

	public void delete(int courseId, int fileId) {
		int number = em.createQuery(
				"SELECT num FROM LectureNote " +
				"WHERE file_id = :fileId", Integer.class)
				.setParameter("fileId", fileId)
				.getSingleResult()
				.intValue();

		String groupname = em.createQuery(
				"SELECT groupname FROM LectureNote " +
				"WHERE file_id = :fileId", String.class)
				.setParameter("fileId", fileId)
				.getSingleResult()
				.toString();

		jdbc.update("UPDATE lecture_note SET num = num - 1 WHERE num > ? AND course_id = ? AND groupname = ?", number, courseId, groupname);

		jdbc.update("DELETE FROM uploaded_file WHERE id = ?", fileId);
	}

	public void rename(int fileId, String title) {
		jdbc.update("UPDATE lecture_note SET title = ? WHERE file_id = ?", title, fileId);
	}

	public void editFile(int courseId, int oldFileId, int newFileId) {
		jdbc.update("UPDATE lecture_note SET file_id = ? WHERE file_id = ?", newFileId, oldFileId);
		delete(courseId, oldFileId);
	}

	@Transactional
	public void move(int id, boolean up) {
		LectureNote ls = em.find(LectureNote.class, id);
		int number = ls.getNum();
		int otherNumber = number + (up ? -1 : 1);

		LectureNote otherLs;
		try {
			// the cast is necessary to compare with the psql enum
			otherLs = em.createQuery(
				"FROM LectureNote WHERE groupname = :groupname " +
				"AND num = :num", LectureNote.class)
				.setParameter("groupname", ls.getGroupname())
				.setParameter("num", otherNumber)
				.getSingleResult();
		} catch(NoResultException e) {
			// already on top/bottom
			return;
		}

		em.createNativeQuery(
			"UPDATE lecture_note dst " +
			"SET num = src.num " +
			"FROM lecture_note src " +
			"WHERE dst.num IN(:number, :otherNumber) " +
			"AND src.num IN(:number, :otherNumber) " +
			"AND dst.num <> src.num " +
			"AND src.groupname =dst.groupname")
			.setParameter("number", ls.getNum())
			.setParameter("otherNumber", otherLs.getNum())
			.executeUpdate();
	}
}
