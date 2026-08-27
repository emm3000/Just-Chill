package com.emm.justchill.hh.shared

import com.emm.domain.shared.YearMonth
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * These tests exist because the app once carried two Spanish month tables — this file's own
 * hand-written one and [SpanishDateFormat]'s — and they disagreed. Home rendered "Setiembre 2026"
 * while the transaction list rendered "septiembre", and the month grid rendered "Set" while a
 * transaction row rendered "sept". Nothing failed; the two tables were simply never compared.
 *
 * [allLabelsComeFromTheOneMonthTable] is that comparison, and it is the point of the suite: it
 * fails the moment a second table reappears here.
 */
class MonthLabelsTest {

    private val september = YearMonth(2026, Month.SEPTEMBER)

    @Test
    fun monthYearLabel_is_the_month_and_year_titlecased() {
        assertEquals("Septiembre 2026", september.monthYearLabel())
        assertEquals("Enero 2026", YearMonth(2026, Month.JANUARY).monthYearLabel())
    }

    @Test
    fun monthLabel_is_the_full_month_titlecased() {
        assertEquals("Septiembre", september.monthLabel())
        assertEquals("Diciembre", YearMonth(2026, Month.DECEMBER).monthLabel())
    }

    @Test
    fun monthAbbrevLabel_is_exactly_three_characters_for_every_month() {
        Month.entries.forEach { month ->
            val label = YearMonth(2026, month).monthAbbrevLabel()
            assertEquals(3, label.length, "Expected a 3-char abbreviation for $month, got \"$label\"")
        }
    }

    @Test
    fun monthAbbrevLabel_truncates_the_four_char_september_abbreviation() {
        // SpanishDateFormat.shortMonth(SEPTEMBER) is "sept" — four characters, and the only month
        // that is. A fixed-width column one character wider than its eleven neighbours reads as a
        // rendering bug, so the grid and the chart axis take three.
        assertEquals("Sep", september.monthAbbrevLabel())
        assertEquals("Ene", YearMonth(2026, Month.JANUARY).monthAbbrevLabel())
    }

    @Test
    fun allLabelsComeFromTheOneMonthTable() {
        Month.entries.forEach { month ->
            val yearMonth = YearMonth(2026, month)
            val canonical = SpanishDateFormat.fullMonth(month)

            assertEquals(
                canonical.titlecaseFirstChar(),
                yearMonth.monthLabel(),
                "monthLabel drifted from SpanishDateFormat for $month",
            )
            assertEquals(
                "${canonical.titlecaseFirstChar()} 2026",
                yearMonth.monthYearLabel(),
                "monthYearLabel drifted from SpanishDateFormat for $month",
            )
            assertEquals(
                SpanishDateFormat.shortMonth(month).take(3).titlecaseFirstChar(),
                yearMonth.monthAbbrevLabel(),
                "monthAbbrevLabel drifted from SpanishDateFormat for $month",
            )
        }
    }
}
