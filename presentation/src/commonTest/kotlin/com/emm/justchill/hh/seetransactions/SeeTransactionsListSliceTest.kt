package com.emm.justchill.hh.seetransactions

import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The rule that keeps the month label and the rows under it describing the same month: the label
 * moves the instant the arrow is tapped, so an emission that answers for a month the user already
 * left must be dropped instead of applied under the new heading.
 */
class SeeTransactionsListSliceTest {

    private val august = YearMonth(2026, Month.AUGUST)
    private val september = YearMonth(2026, Month.SEPTEMBER)

    private fun day(date: LocalDate) = DayGroup(date = date, today = date, transactions = emptyList())

    private val augustDays = listOf(day(LocalDate(2026, 8, 2)))
    private val septemberDays = listOf(day(LocalDate(2026, 9, 2)))
    private val augustSummary = MonthSummaryUi(income = Money(1_000), spend = Money(250))

    private val showingSeptember = SeeTransactionsUiState(
        month = september,
        days = septemberDays,
        summary = MonthSummaryUi(income = Money(9), spend = Money(9)),
        movementCount = 9,
    )

    @Test fun a_slice_for_the_selected_month_replaces_the_rows_and_the_summary() {
        val fresh = ListSlice(month = august, days = augustDays, summary = augustSummary)

        val next = SeeTransactionsUiState(month = august, movementCount = 9).withListSlice(fresh, august)

        assertEquals(augustDays, next.days)
        assertEquals(augustSummary, next.summary)
    }

    @Test fun a_month_mode_slice_for_a_month_the_user_already_left_is_dropped() {
        val stale = ListSlice(month = august, days = augustDays, summary = augustSummary)

        val next = showingSeptember.withListSlice(stale, september)

        assertEquals(showingSeptember, next)
    }

    @Test fun a_search_slice_always_applies_because_its_results_cross_months() {
        val results = ListSlice(month = null, days = augustDays, summary = null)

        val next = showingSeptember.withListSlice(results, september)

        assertEquals(augustDays, next.days)
        assertNull(next.summary)
    }

    @Test fun applying_a_slice_never_moves_the_label_itself() {
        val fresh = ListSlice(month = september, days = septemberDays, summary = null)

        val next = SeeTransactionsUiState(month = september).withListSlice(fresh, september)

        assertEquals(september, next.month)
    }
}
