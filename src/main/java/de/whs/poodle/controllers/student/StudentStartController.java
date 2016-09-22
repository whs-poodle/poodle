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
package de.whs.poodle.controllers.student;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.CourseTermWorksheets;
import de.whs.poodle.beans.ExerciseWorksheet;
import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.Worksheet;
import de.whs.poodle.beans.evaluation.EvaluationWorksheet;
import de.whs.poodle.beans.mc.InstructorMcWorksheet;
import de.whs.poodle.beans.statistics.Statistic;
import de.whs.poodle.repositories.EvaluationWorksheetRepository;
import de.whs.poodle.repositories.FeedbackRepository;
import de.whs.poodle.repositories.McStatisticsRepository;
import de.whs.poodle.repositories.StatisticsRepository;
import de.whs.poodle.repositories.WorksheetRepository;

@Controller
@RequestMapping("/student")
public class StudentStartController {

	@Autowired
	private WorksheetRepository worksheetRepo;

	@Autowired
	private EvaluationWorksheetRepository evaluationWorksheetRepo;

	@Autowired
	private FeedbackRepository feedbackRepo;

	@Autowired
	private McStatisticsRepository mcStatisticsRepo;

	@Autowired
	private StatisticsRepository statisticsRepo;

	@RequestMapping(method = RequestMethod.GET)
	public String get(@ModelAttribute Student student, Model model, @RequestParam(defaultValue="0") boolean evaluationSaved) {
		// get all course terms that the student is enrolled in and all the worksheets in them
		HashMap<CourseTerm, CourseTermWorksheets> courseTermWorksheetsMap = worksheetRepo.getWorksheetsForStudent(student.getId());

		// create lists of all the exercise / mc worksheet so we can create the "is completed" maps
		List<ExerciseWorksheet> allExerciseWorksheets = courseTermWorksheetsMap.values().stream()
				.flatMap(w -> w.getExerciseWorksheets().stream())
				.collect(Collectors.toList());

		List<InstructorMcWorksheet> allMcWorksheets = courseTermWorksheetsMap.values().stream()
				.flatMap(w -> w.getMcWorksheets().stream())
				.collect(Collectors.toList());

		List<EvaluationWorksheet> allEvaluationWorksheets = courseTermWorksheetsMap.values().stream()
				.map(ctws -> ctws.getEvaluationWorksheet())
				.filter(ws -> ws != null)
				.collect(Collectors.toList());

		// maps that define whether the student has completed the worksheet
		Map<ExerciseWorksheet,Boolean> exerciseWorksheetIsCompletedMap =
				feedbackRepo.getExerciseWorksheetIsCompletedMap(student.getId(), allExerciseWorksheets);

		Map<InstructorMcWorksheet,Boolean> mcWorksheetIsCompletedMap =
				mcStatisticsRepo.getInstructorMcWorksheetIsCompletedMap(student.getId(), allMcWorksheets);

		Map<EvaluationWorksheet,Boolean> evaluationIsCompletedMap =
				evaluationWorksheetRepo.getEvaluationIsCompletedMap(student.getId(), allEvaluationWorksheets);

		Map<Worksheet,Boolean> worksheetIsCompletedMap = new HashMap<>();
		worksheetIsCompletedMap.putAll(exerciseWorksheetIsCompletedMap);
		worksheetIsCompletedMap.putAll(mcWorksheetIsCompletedMap);
		worksheetIsCompletedMap.putAll(evaluationIsCompletedMap);

		// Exercises that contain new comments by an instructor
		List<Statistic> statisticsWithNewComments = statisticsRepo.getStatisticsWithNewCommentsForStudent(student.getId());

		model.addAttribute("courseTermWorksheetsMap", courseTermWorksheetsMap);
		model.addAttribute("worksheetIsCompletedMap", worksheetIsCompletedMap);
		model.addAttribute("statisticsWithNewComments", statisticsWithNewComments);

		if (evaluationSaved)
			model.addAttribute("okMessageCode", "evaluationSaved");

		return "student/start";
	}
}
