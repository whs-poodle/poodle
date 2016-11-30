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
package de.whs.poodle.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/*
 * Base class for both "Exercise" and "McQuestion".
 */
@MappedSuperclass
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractExercise {

	// we pass this to some functions to differentiate between the types
	public enum ExerciseType {
		ALL, EXERCISE, MC_QUESTION
	}

	public enum Visibility {
		// must be in sync with enum in database!
		PUBLIC, PRIVATE;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="owner_id")
	private Instructor owner;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="changed_by_id")
	private Instructor changedBy;

	@Column(name="text")
	private String text;

	@Column(name="created_at")
	private Date createdAt;

	@ManyToMany
	private List<Tag> tags;

	@Column(name="root_id")
	private int rootId;

	// Integer because it is null if this is the first revision
	@Column(name="previous_revision_id")
	private Integer previousRevisionId;

	@Column(name="course_id")
	private int courseId;

	@Column(name="is_latest_revision")
	private boolean latestRevision;

	@Enumerated(EnumType.STRING)
	@Column(name="visibility")
	private Visibility visibility;

	@Column(name="comment")
	private String comment;

	public AbstractExercise() {
		this.tags = new ArrayList<>();
		this.visibility = Visibility.PUBLIC;
	}

	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		if (tags == null)
			this.tags = new ArrayList<>();
		else
			this.tags = tags;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getRootId() {
		return rootId;
	}

	public void setRootId(int rootId) {
		this.rootId = rootId;
	}

	public Integer getPreviousRevisionId() {
		return previousRevisionId;
	}

	public void setPreviousRevisionId(Integer previousRevisionId) {
		this.previousRevisionId = previousRevisionId;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Instructor getOwner() {
		return owner;
	}

	public void setOwner(Instructor owner) {
		this.owner = owner;
	}

	public Instructor getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(Instructor changedBy) {
		this.changedBy = changedBy;
	}

	public int getCourseId() {
		return courseId;
	}

	public void setCourseId(int courseId) {
		this.courseId = courseId;
	}

	public boolean isLatestRevision() {
		return latestRevision;
	}

	public void setLatestRevision(boolean latestRevision) {
		this.latestRevision = latestRevision;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}

	public abstract String getTitle();

	public boolean isRoot() {
		return id == rootId;
	}

	public String getComment() {
		return this.comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
}
