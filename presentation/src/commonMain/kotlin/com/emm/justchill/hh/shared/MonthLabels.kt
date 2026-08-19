package com.emm.justchill.hh.shared

import com.emm.domain.shared.YearMonth

private const val ABBREV_LENGTH = 3

fun YearMonth.monthYearLabel(): String = SpanishDateFormat.monthYear(year, month).titlecaseFirstChar()

fun YearMonth.monthLabel(): String = SpanishDateFormat.fullMonth(month).titlecaseFirstChar()

/** Three characters exactly — the fixed-width slots cannot take a wider column. */
fun YearMonth.monthAbbrevLabel(): String = SpanishDateFormat.shortMonth(month).take(ABBREV_LENGTH).titlecaseFirstChar()
