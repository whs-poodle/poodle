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
package de.whs.poodle.controllers.instructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import de.whs.poodle.beans.Chapter;
import de.whs.poodle.beans.Chapter.ExerciseInChapter;
import de.whs.poodle.beans.CompletionStatus;
import de.whs.poodle.beans.CourseTerm;
import de.whs.poodle.beans.DataTablesAjaxResponse;
import de.whs.poodle.beans.ExerciseWorksheet;
import de.whs.poodle.beans.Student;
import de.whs.poodle.beans.feedbackOverview.FeedbackOverviewDataTablesRequest;
import de.whs.poodle.beans.feedbackOverview.FeedbackOverviewDataTablesRequest.OrderDirection;
import de.whs.poodle.beans.feedbackOverview.FeedbackOverviewRowData;
import de.whs.poodle.beans.feedbackOverview.FeedbackOverviewValueCell;
import de.whs.poodle.beans.forms.FeedbackOverviewForm;
import de.whs.poodle.beans.forms.FeedbackOverviewForm.VisibleValue;
import de.whs.poodle.beans.statistics.Statistic;
import de.whs.poodle.beans.statistics.StatisticList;
import de.whs.poodle.repositories.CourseTermRepository;
import de.whs.poodle.repositories.ExerciseWorksheetRepository;
import de.whs.poodle.repositories.StatisticsRepository;

@Controller
@RequestMapping("/instructor/courses/{courseId}/feedbackOverview")
public class FeedbackOverviewController {

	@Autowired
	private CourseTermRepository courseTermRepo;

	@Autowired
	private StatisticsRepository statisticsRepo;

	@Autowired
	private ExerciseWorksheetRepository exerciseWorksheetRepo;

	@RequestMapping
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String get(@PathVariable int courseId, FeedbackOverviewForm feedbackOverviewForm, Model model) {
		List<CourseTerm> courseTerms = courseTermRepo.getForCourse(courseId);
		CourseTerm courseTerm;

		// if there is no courseTerm specified in the form, use the latest of the course
		if (feedbackOverviewForm.getCourseTermId() == 0) {
			courseTerm = courseTerms.get(0);
			feedbackOverviewForm.setCourseTermId(courseTerm.getId());
		}
		else {
			courseTerm = courseTerms.stream()
				.filter(ct -> ct.getId() == feedbackOverviewForm.getCourseTermId())
				.findFirst().get();
		}

		List<ExerciseWorksheet> worksheets = exerciseWorksheetRepo.getUnlockedForCourseTerm(courseTerm.getId());

		// get all statistics for the worksheets in this courseTerm
		List<Statistic> courseTermStatistics = statisticsRepo.getForWorksheetsInCourseTerm(courseTerm.getId());

		AbstractHelper helper = getHelper(courseTermStatistics, feedbackOverviewForm.getValue());

		model.addAttribute("courseTerms", courseTerms);
		model.addAttribute("courseTerm", courseTerm);
		model.addAttribute("worksheets", worksheets);
		model.addAttribute("helper", helper);

		return "instructor/feedbackOverview";
	}

	/*
	 * This is used by the DataTables server side processing API
	 * to get the data for the table. The request that DataTables sends
	 * (see feedbackOverview.js) is mapped to the FeedbackOverviewDataTablesRequest object.
	 * It contains all the data we need to get, sort and return the correct table rows.
	 *
	 * See https://www.datatables.net/manual/server-side
	 */
	@RequestMapping(value="tableData")
	@ResponseBody
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public DataTablesAjaxResponse getData(@PathVariable int courseId, @ModelAttribute FeedbackOverviewDataTablesRequest dataTablesRequest) {
		// get all statistics for the worksheets in this courseTerm
		List<Statistic> courseTermStatistics = statisticsRepo.getForWorksheetsInCourseTerm(dataTablesRequest.getCourseTermId());

		// helper object we use to calculate the color etc. for the table cells (see AbstractHelper)
		AbstractHelper helper = getHelper(courseTermStatistics, dataTablesRequest.getValue());

		// create the data rows from the statistics
		List<FeedbackOverviewRowData> rows = createRows(courseTermStatistics);

		// sort them
		Comparator<FeedbackOverviewRowData> comparator = getRowComparator(dataTablesRequest, helper);
		Collections.sort(rows, comparator);

		// return the rows to DataTables
		return createDataTablesResponse(dataTablesRequest, rows, helper);
	}

	/*
	 * Creates the data for the table rows from all the statistics for this course term.
	 *
	 * The table has the students as the rows and the exercises as the columns.
	 * To create the table, we therefore need a list of rows, with each row
	 * belonging to a particular student and containing the statistics for
	 * each exercise on the worksheets (see FeedbackOverviewTableRowData).
	 */
	private List<FeedbackOverviewRowData> createRows(List<Statistic> courseTermStatistics) {
		// map from the studentId to all of the student's statistics
		Map<Student,List<Statistic>> studentToStatisticsMap = courseTermStatistics.stream()
			.collect(
				Collectors.groupingBy(Statistic::getStudent)
			);

		// create the rows from the studentToStatisticsMap
		List<FeedbackOverviewRowData> rows = new ArrayList<>();

		for (Map.Entry<Student, List<Statistic>> e : studentToStatisticsMap.entrySet()) {
			Student student = e.getKey();
			List<Statistic> studentStatistics = e.getValue();

			/* studentStatistics is now the list of all statistics that the student
			 * has created in this courseTerm. We now transform this list into a
			 * map which maps each exercise to its statistic (a student can only
			 * ever create one statistic for each exercise). */
			Map<Integer,Statistic> rootIdToStatisticMap =
						studentStatistics.stream().
						collect(
							Collectors.toMap(
									Statistic::getExerciseRootId, Function.identity()
							)
						);

			// create the row
			rows.add(new FeedbackOverviewRowData(student, rootIdToStatisticMap));
		}

		return rows;
	}

	/* Returns the Comparator we use to sort the rows according to
	 * what DataTables sent us in the request. */
	public Comparator<FeedbackOverviewRowData> getRowComparator(FeedbackOverviewDataTablesRequest dataTablesRequest, AbstractHelper helper) {
		Comparator<FeedbackOverviewRowData> comparator;

		// compare by students
		if (dataTablesRequest.isOrderByStudent()) {
			comparator = (r1,r2) -> r1.getStudent().getId() - r2.getStudent().getId();
		}
		else { // compare by exercise
			int exerciseRootId = dataTablesRequest.getOrderByExerciseRootId();

			comparator = (r1,r2) -> {
				// note that each of these may be null if the student has not completed the exercise
				Statistic s1 = r1.getExerciseRootIdToStatisticMap().get(exerciseRootId);
				Statistic s2 = r2.getExerciseRootIdToStatisticMap().get(exerciseRootId);

				return helper.getOrderValue(s1) - helper.getOrderValue(s2);
			};
		}

		// if we have to sort descending, reverse the comparator
		if (dataTablesRequest.getOrderDirection() == OrderDirection.DESC)
			comparator = Collections.reverseOrder(comparator);

		return comparator;
	}

	/*
	 * Create the DataTables Response we return to dataTables.
	 * Since we also have to color the cells etc. we cannot simply
	 * send DataTables the plain values in the "data" attribute.
	 * Instead we send a List<Object> for each row with the elements
	 * in the list being the data for the cells (Integer for the
	 * first column (student id) and FeedbackOverviewValueCell for
	 * the cells with the actual values).
	 */
	private DataTablesAjaxResponse createDataTablesResponse(
			FeedbackOverviewDataTablesRequest dataTablesRequest,
			List<FeedbackOverviewRowData> rows,
			AbstractHelper helper) {

		/* Create the sublist for the rows that DataTables wants to display.
		 * Note that the rows are already sorted at this point. */
		int studentCount = rows.size();

		int fromIndex = dataTablesRequest.getStart();
		int toIndex = fromIndex + dataTablesRequest.getLength();
		if (toIndex >= studentCount)
			toIndex = studentCount;

		List<FeedbackOverviewRowData> rowsToReturn = rows.subList(fromIndex, toIndex);

		// create the row data that DataTables uses to render the cells
		List<List<Object>> data = new ArrayList<>();

		List<ExerciseWorksheet> worksheets = exerciseWorksheetRepo.getUnlockedForCourseTerm(dataTablesRequest.getCourseTermId());

		for (FeedbackOverviewRowData row : rowsToReturn) {
			List<Object> rowCells = new ArrayList<>();

			// the first cell in the row is the student
			rowCells.add(row.getStudent());

			// the rest of the cells are for the exercises
			for (ExerciseWorksheet ws : worksheets) {
				for (Chapter c : ws.getChapters()) {
					for (ExerciseInChapter e : c.getExercises()) {
						int rootId = e.getExercise().getRootId();

						// may be null if the student has not completed this exercise
						Statistic statistic = row.getExerciseRootIdToStatisticMap().get(rootId);

						// create the data that DataTables needs to render the cell
						FeedbackOverviewValueCell cell = new FeedbackOverviewValueCell();
						cell.setStatistic(statistic);
						cell.setCssColor(helper.getCSSColor(statistic));
						cell.setDisplayValue(helper.getDisplayValue(statistic));

						rowCells.add(cell);
					}
				}
			}

			data.add(rowCells);
		}

		// create the complete response

		DataTablesAjaxResponse response = new DataTablesAjaxResponse();
		response.setDraw(dataTablesRequest.getDraw());
		response.setRecordsTotal(studentCount);
		// "filtered" means the text based filter built into DataTables which we don't use
		response.setRecordsFiltered(studentCount);
		response.setData(data);

		return response;
	}

	// this is called via Ajax when a value is clicked to get the content for the popover
	@RequestMapping("popoverContent/{statisticId}")
	@PreAuthorize("@instructorSecurity.hasAccessToCourse(authentication.name, #courseId)")
	public String getStatisticPopoverContent(@PathVariable int courseId, @PathVariable int statisticId, Model model) {
		Statistic statistic = statisticsRepo.getById(statisticId);
		model.addAttribute("statistic", statistic);
		return "fragments/feedbackOverviewPopoverContent";
	}

	// return the correct helper, depending on which value we are currently displaying
	private AbstractHelper getHelper(List<Statistic> courseTermStatistics, VisibleValue value) {
		switch (value) {
		case DIFFICULTY:
			return new DifficultyHelper(courseTermStatistics);
		case TIME:
			return new TimeHelper(courseTermStatistics);
		case FUN:
			return new FunHelper(courseTermStatistics);
		case COMPLETED:
			return new StatusHelper(courseTermStatistics);
		default:
			throw new RuntimeException("invalid value");
		}
	}

	/*
	 * this is the base class for the helper object we use to calculate
	 * some values for the table (see abstract functions).
	 * The actual implementation depends on which value we are displaying (time or fun etc.),
	 * see the other classes extending this class below.
	 */
	private abstract class AbstractHelper {

		/* defines the hue range in the HSL color model
		 * we use to generate the colors for the cells (see createCSSColor()).
		 * Note: 0 would be completely red, 120 would be completely green */
		private static final int HUE_MIN = 30;
		private static final int HUE_MAX = 90;

		protected Map<Integer,List<Statistic>> firstRevisionIdToStatisticsMap;

		/* defines whether we should/can show average values in the table footer,
		 * e.g. average values for the completion status don't really make sense. */
		private boolean hasAverage;

		public AbstractHelper(List<Statistic> courseTermStatistics, boolean hasAverage) {
			/* map which maps each exercise to its statistics. We need this calculate
			 * the average values and other things. */
			this.firstRevisionIdToStatisticsMap = courseTermStatistics.stream()
					.collect(
							Collectors.groupingBy(Statistic::getExerciseRootId)
					);
			this.hasAverage = hasAverage;
		}

		/*
		 * This is used by the implementations of getCSSColor() to
		 * create the CSS style for a value. For example, to get
		 * the color value for a "difficulty" value, we would pass
		 * (difficulty, 1, 10) as parameters and get a CSS style
		 * representing the value on the specified scale.
		 */
		protected String createCSSColor(int n, int min, int max) {
			/* We use the HSL color model representation to calculate
			 * the color. In this implementation the saturation and
			 * lightness are fixed and we only vary the hue value, e.g.
			 * if n == min, we would return HUE_MIN and if n == max, we
			 * would return HUE_MAX etc. */
			int hue;

			if (min == max) // if min and max are the same, show the "middle" color for all values
				hue = (HUE_MIN + HUE_MAX) / 2;
			else {
				/* transform the value n from the range min<->max to
				 * the hue value in the range HUE_MIN<->HUE_MAX.
				 * Don't ask me how this formula works exactly, it just does. */
				double dMin = min, dMax = max, dn = n; // convert everything into double to avoid integer division
				hue = (int)(((dn - dMin) / (dMax - dMin)) * (HUE_MAX - HUE_MIN) + HUE_MIN);
			}

			// return CSS style value with the calculated hue and a fixed saturation and lightness
			return String.format("background-color : hsl(%s, 100%%, 50%%)", hue);
		}

		public boolean isHasAverage() {
			return hasAverage;
		}

		public String getAverageString(int firstRevisionId) {
			if (!hasAverage)
				throw new UnsupportedOperationException();

			List<Statistic> statistics = firstRevisionIdToStatisticsMap.get(firstRevisionId);
			if (statistics == null)
				return "-";

			StatisticList statisticList = new StatisticList(statistics);
			return getAverageForStatisticList(statisticList);
		}

		// should return the CSS color for the statistic, empty string if none
		public abstract String getCSSColor(Statistic s);

		/* Should return the value for the "data-order" attribute on the table
		 * cell. This is used by DataTables so sort the columns correctly. */
		public abstract int getOrderValue(Statistic s);

		/* Should return the String that is actually displayed in the table cell. */
		public abstract String getDisplayValue(Statistic s);

		/* Should return the "average" string displayed in the table footer.
		 * Note that the HTML template calls getAverageString() instead of
		 * this function since the former takes care of some common logic. */
		protected abstract String getAverageForStatisticList(StatisticList statisticList);
	}

	/*
	 * Helper used if we display the difficulty values.
	 */
	public class DifficultyHelper extends AbstractHelper {

		public DifficultyHelper(List<Statistic> courseStermStatistics) {
			super(courseStermStatistics, true);
		}

		@Override
		public String getCSSColor(Statistic s) {
			if (s == null || s.getDifficulty() == null)
				return "";

			// swap min and max parameters since we want "higher = worse"
			return createCSSColor(s.getDifficulty(), 10, 1);
		}

		@Override
		public int getOrderValue(Statistic s) {
			if (s == null || s.getDifficulty() == null)
				return 0;

			return s.getDifficulty();
		}

		@Override
		public String getDisplayValue(Statistic s) {
			if (s == null || s.getDifficulty() == null)
				return "-";

			return String.valueOf(s.getDifficulty());
		}

		@Override
		protected String getAverageForStatisticList(StatisticList statisticList) {
			return statisticList.getAvgDifficultyStr();
		}
	}

	/*
	 * Helper used if we display the time values.
	 */
	public class TimeHelper extends AbstractHelper {

		private Map<Integer,Integer> minMap = new HashMap<>();
		private Map<Integer,Integer> maxMap = new HashMap<>();

		public TimeHelper(List<Statistic> courseStermStatistics) {
			super(courseStermStatistics, true);

			/* For all exercises, calculate the min and max time values and
			 * store them in minMap and maxMap.
			 * We need these values to calculate the CSS color (see getCSSColor())
			 * and calculating the values there would be inefficient since it is
			 * called multiple times for the same exercise (once for each student) */
			for (Map.Entry<Integer,List<Statistic>> e: firstRevisionIdToStatisticsMap.entrySet()) {
				int exerciseRootId = e.getKey();
				List<Statistic> statistics = e.getValue();

				int min = statistics.stream()
						.filter(s -> s.getTime() != null && !s.isIgnore())
						.mapToInt(s -> s.getTime())
						.min().orElse(0);

				int max = statistics.stream()
						.filter(s -> s.getTime() != null && !s.isIgnore())
						.mapToInt(s -> s.getTime())
						.max().orElse(0);

				minMap.put(exerciseRootId, min);
				maxMap.put(exerciseRootId, max);
			}
		}

		@Override
		public String getCSSColor(Statistic s) {
			if (s == null || s.getTime() == null)
				return "";

			int min = minMap.get(s.getExerciseRootId());
			int max = maxMap.get(s.getExerciseRootId());

			// swap min and max parameters since we want "higher = worse"
			return createCSSColor(s.getTime(), max, min);
		}

		@Override
		public int getOrderValue(Statistic s) {
			if (s == null || s.getTime() == null)
				return 0;

			return s.getTime();
		}

		@Override
		public String getDisplayValue(Statistic s) {
			if (s == null || s.getTime() == null)
				return "-";

			return String.valueOf(s.getTime());
		}

		@Override
		protected String getAverageForStatisticList(StatisticList statisticList) {
			return statisticList.getAvgTimeStr();
		}
	}

	/*
	 * Helper used if we display the fun values.
	 */
	public class FunHelper extends AbstractHelper {

		public FunHelper(List<Statistic> courseStermStatistics) {
			super(courseStermStatistics, true);
		}

		@Override
		public String getCSSColor(Statistic s) {
			if (s == null || s.getFun() == null)
				return "";

			return createCSSColor(s.getFun(), 1, 10);
		}

		@Override
		public int getOrderValue(Statistic s) {
			if (s == null || s.getFun() == null)
				return 0;

			return s.getFun();
		}

		@Override
		public String getDisplayValue(Statistic s) {
			if (s == null || s.getFun() == null)
				return "-";

			return String.valueOf(s.getFun());
		}

		@Override
		protected String getAverageForStatisticList(StatisticList statisticList) {
			return statisticList.getAvgFunStr();
		}
	}

	/*
	 * Helper used if we display the status values.
	 */
	public class StatusHelper extends AbstractHelper {

		public StatusHelper(List<Statistic> courseStermStatistics) {
			super(courseStermStatistics, false);
		}

		@Override
		public String getCSSColor(Statistic s) {
			if (s == null)
				return "-";

			return getCSSColorForStatus(s.getStatus());
		}

		// own function since we need it for the legend
		public String getCSSColorForStatus(CompletionStatus status) {
			int min = 1;
			int max = CompletionStatus.values().length;

			if (status == null)
				return "";

			switch (status) {
			case COMPLETELY:
				return createCSSColor(3, min, max);

			case PARTLY:
				return createCSSColor(2, min, max);

			case NOTATALL:
				return createCSSColor(1, min, max);

			default:
				throw new RuntimeException("invalid status");
			}
		}

		@Override
		public int getOrderValue(Statistic s) {
			if (s == null)
				return 0;

			return s.getStatus().ordinal();
		}

		@Override
		public String getDisplayValue(Statistic s) {
			if (s == null || s.getStatus() == null)
				return "-";

			/* We don't display the actual value since that would take too much space.
			 * We show a legend above the table instead. */
			return "";
		}

		/* Average doesn't make sense here. This is never called
		 * since we set hasAverage=false in the constructor and the
		 * HTML template checks for that. */
		@Override
		protected String getAverageForStatisticList(StatisticList statisticList) {
			throw new UnsupportedOperationException();
		}
	}
}
