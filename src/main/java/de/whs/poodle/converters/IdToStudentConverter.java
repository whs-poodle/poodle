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

import de.whs.poodle.beans.Student;
import de.whs.poodle.repositories.StudentRepository;

/*
 * Used by Spring to automatically convert an ID to a student. Used in some forms
 * where the form only contains the ID but the form object contains the student object.
 */
@Component
public class IdToStudentConverter implements Converter<String,Student> {

	private static Logger log = LoggerFactory.getLogger(IdToStudentConverter.class);

	@Autowired
	private StudentRepository studentRepo;

	@Override
	public Student convert(String id) {
		log.debug("converting id '{}' to student", id);
		return studentRepo.getById(Integer.parseInt(id));
	}


}
