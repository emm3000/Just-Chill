package com.emm.justchill.hh.seetransactions

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden assertions for the day-header hierarchy of the transactions tab: the date is the
 * prominent label ("Martes 13"), the month/year is a caption the search mode adds back.
 *
 * Every date here is fixed, including the reference "today" — it is an input to [DayGroup], not an
 * ambient clock read, so a run that crosses midnight cannot change any answer below.
 */
class DayGroupTest {

    private val today = LocalDate(2026, 8, 10)

    private fun group(date: LocalDate, reference: LocalDate = today) =
        DayGroup(date = date, today = reference, transactions = emptyList())

    @Test fun primaryLabel_today_is_HOY() {
        assertEquals("HOY", group(today).primaryLabel)
    }

    @Test fun primaryLabel_yesterday_is_AYER() {
        assertEquals("AYER", group(LocalDate(2026, 8, 9)).primaryLabel)
    }

    @Test fun primaryLabel_yesterday_across_a_month_boundary_is_AYER() {
        assertEquals("AYER", group(LocalDate(2026, 7, 31), reference = LocalDate(2026, 8, 1)).primaryLabel)
    }

    @Test fun primaryLabel_tomorrow_is_not_HOY_nor_AYER() {
        assertEquals("Martes 11", group(LocalDate(2026, 8, 11)).primaryLabel)
    }

    @Test fun primaryLabel_regular_day_is_titlecase_weekday_plus_day_number() {
        assertEquals("Martes 13", group(LocalDate(2026, 1, 13)).primaryLabel)
        assertEquals("Miércoles 4", group(LocalDate(2026, 3, 4)).primaryLabel)
    }

    @Test fun monthYearCaption_is_lowercase_month_plus_year() {
        assertEquals("agosto 2026", group(LocalDate(2026, 8, 5)).monthYearCaption)
        assertEquals("diciembre 2025", group(LocalDate(2025, 12, 31)).monthYearCaption)
    }
}
