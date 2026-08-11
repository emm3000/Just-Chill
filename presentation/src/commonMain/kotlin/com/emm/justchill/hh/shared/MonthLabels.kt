package com.emm.justchill.hh.shared

import com.emm.domain.shared.YearMonth

private const val ABBREV_LENGTH = 3

// Display labels for a YearMonth. Every one of them delegates to SpanishDateFormat, which owns the
// only Spanish month table in the app.
//
// This file used to carry a second, hand-written table, and the two disagreed: it spelled September
// "Setiembre" where SpanishDateFormat — pinned by SpanishFormatGoldenTest to the `es-PE` output the
// JVM formatters produced — spells it "septiembre". Both reached the screen, so Home read
// "Setiembre 2026" while the transaction list read "septiembre", and the month grid read "Set"
// while a transaction row read "sept". Nothing failed, because nothing compared them.
//
// MonthLabelsTest compares them now. Adding a month name to this file instead of to
// SpanishDateFormat fails it.

/** "Septiembre 2026" — the month selector on Home, Reporte and the transactions tab. */
fun YearMonth.monthYearLabel(): String = SpanishDateFormat.monthYear(year, month).titlecaseFirstChar()

/** "Septiembre" — the month on its own, for prose that already supplies the year. */
fun YearMonth.monthLabel(): String = SpanishDateFormat.fullMonth(month).titlecaseFirstChar()

/**
 * "Sep" — exactly three characters, for the fixed-width slots: the twelve-tile month grid and the
 * trends chart axis.
 *
 * Truncated from [SpanishDateFormat.shortMonth] rather than given a table of its own. That
 * abbreviation is three characters for eleven months and four for September ("sept"), and one
 * column a character wider than its eleven neighbours reads as a rendering bug.
 */
fun YearMonth.monthAbbrevLabel(): String = SpanishDateFormat.shortMonth(month).take(ABBREV_LENGTH).titlecaseFirstChar()
