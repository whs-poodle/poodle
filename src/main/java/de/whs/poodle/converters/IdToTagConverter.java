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
package de.whs.poodle.converters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import de.whs.poodle.beans.Tag;
import de.whs.poodle.repositories.TagRepository;

/*
 * Used by Spring to automatically map a tag id to the tag. This is for example
 * used when saving an exercise (the form only contains the IDs which are mapped
 * to the List<Tag> in the Exercise object).
 */
@Component
public class IdToTagConverter implements Converter<String,Tag> {

	private static Logger log = LoggerFactory.getLogger(IdToTagConverter.class);

	@Autowired
	private TagRepository tagRepo;

	@Override
	public Tag convert(String tagId) {
		log.debug("converting id '{}' to tag", tagId);
		return tagRepo.getById(Integer.parseInt(tagId));
	}

}
