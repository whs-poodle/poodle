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
/* exported messages */
/* jshint -W097 */
"use strict";

var messages = {
	// global.js
	close : "schließen",
	ajaxError : "Fehler bei Anfrage ({0})",

	// Localization file for DataTables (see https://datatables.net/plug-ins/i18n/)
	dataTablesLanguageUrl : "https://cdn.datatables.net/plug-ins/a5734b29083/i18n/German.json",

	// CKeditor language
	CKEditorLanguage : "de",

	googleChartsLocale : "de",

	dateTimePickerLocale : "de",

	momentJsLocale : "de",

	/* Format for the Bootstrap DateTimePicker values.
	 * This pattern must match the server side pattern for "dateTimeFormat".
	 * See http://momentjs.com/docs/#/displaying/format/ */
	dateTimePickerFormat : "DD.MM.YY, HH:mm",

	// CKeditor "link exercise" plugin
	linkExercise : "Aufgabe verlinken",

	// exerciseCommon.js
	reallyDeleteThisExercise : "Diese Aufgabe inklusive aller ihrer Revisionen wirklich löschen? Dies kann nicht rückgängig gemacht werden.",

	// worksheets.js
	reallyDeleteWorksheet : "\"{0}\" wirklich löschen? Dies kann nicht rückgängig gemacht werden.",
	reallyUnlockWorksheet : "\"{0}\" wirklich freischalten? Es können danach keine weiteren Änderungen am Aufgabenblatt vorgenommen werden.",

	// worksheetEditor.js
	reallyRemoveChapter : "\"{0}\" wirklich entfernen? Dies kann nicht rückgängig gemacht werden.",

	// courseStatistics.js
	day : "Tag",
	worksheets : "Aufgabenblätter",
	selfStudy : "Selbststudium",
	total : "gesamt",
	feedbackTotal : "Feedback (gesamt)",

	/* date format in chart tooltip.
	 * http://momentjs.com/docs/#/displaying/format/ */
	statsJsDayFormat : "dd, DD.MM.",

	// tags.js
	reallyRemoveTag : "Dieses Tag wirklich aus dem Modul entfernen? Wenn noch Aufgaben existieren, die dieses Tag verwenden, so wird das Tag von diesen entfernt.",
	tagRenamed : "Tag wurde umbenannt.",
	tagRemoved : "Tag wurde entfernt.",
	tagCreated : "Tag wurde erstellt.",
	min2Tags : "Sie müssen mindestens zwei Tags auswählen, um sie zusammenfassen zu können.",
	tagsMerged : "Tags wurden zusammengefasst.",

	// mcQuestionEditor.js
	tooManyAnswers : "Maximal {0} Antworten möglich.",
	tooFewAnswers : "Mindestens {0} Antworten nötig.",

	// answerMcQuestion.js
	reallyCancelWorksheet : "Fragebogen wirklich abbrechen? Dies kann nicht rückgängig gemacht werden.",
	noAnswerSelected : "Keine Antwort ausgewählt.",

	// exerciseSearch.js
	exerciseDeleted : "Aufgabe gelöscht.",
	exerciseAdded : "Aufgabe wurde hinzugefügt.",
	reallyDeleteExercise : "\"{0}\" inklusive aller Revisionen wirklich löschen? Dies kann nicht rückgängig gemacht werden.",
	oneExerciseLeft : "Aufgabe wurde hinzugefügt. Du kannst diesem Übungsblatt noch eine Aufgabe hinzufügen.",
	nExercisesLeft : function(n) {
		return "Aufgabe wurde hinzugefügt. Du kannst diesem Übungsblatt noch " +
			(n === 1 ? "eine Aufgabe" : n + " Aufgaben") + " hinzufügen.";
	},

	// mcQuestionSearch.js
	questionDeleted : "Frage wurde gelöscht.",

	// multipleChoice.js
	nQuestions : function(n) {
		if (n === 0)
			return "keine Fragen";
		else if (n === 1)
			return "eine Frage";
		else
			return n + " Fragen";
	},

	// exercise.js
	reallyDeleteTimeValue : "Diesen Wert wirklich löschen? Dies kann nicht mehr rückgängig gemacht werden.",
	count : "Anzahl",
	completed : "bearbeitet",
	fun : "Spaß",
	time : "Zeitaufwand",
	difficulty : "Schwierigkeit",
	difficultyTitle : "Schwierigkeit (⌀ {0})",
	funTitle : "Spaß (⌀ {0})",
	timeTitle : "Zeitaufwand (⌀ {0})",
	completedStatus : {
		COMPLETELY : "komplett",
		PARTLY : "teilweise",
		NOTATALL : "gar nicht"
	},

	// instructor/mcWorksheet.js
	answer : "Antwort",

	// student/evaluation.js
	missingChoice : "Du hast nicht alle Fragen beantwortet.",

	// instructor/evalution.js
	averageN : "⌀ {0}",

	// mcWorksheetHighscoreChart.js
	highscoreChartTitle: "Punkteverteilung aller Studenten",
	points : "Punkte"
};
