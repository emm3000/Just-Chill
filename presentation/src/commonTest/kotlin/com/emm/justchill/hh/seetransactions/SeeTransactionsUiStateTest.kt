package com.emm.justchill.hh.seetransactions

import com.emm.domain.shared.YearMonth
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The list's empty/loading precedence is decided here, not in a UI `when`. Every consumer of
 * [SeeTransactionsUiState] — Android today, SwiftUI next — reads one value and renders it, so
 * these cases are the whole contract.
 */
class SeeTransactionsUiStateTest {

    // Stated, not read. The state used to default `month` to YearMonth.current(), which quietly
    // made every case below depend on the day it ran; none of them is about a month at all.
    private val august = YearMonth(2026, Month.AUGUST)

    private val day = DayGroup(
        date = LocalDate(2026, 8, 10),
        today = LocalDate(2026, 8, 10),
        transactions = emptyList(),
    )

    @Test fun unknown_count_is_loading_not_an_empty_state() {
        val state = SeeTransactionsUiState(month = august)

        assertEquals(ListDisplayState.Loading, state.listDisplayState)
    }

    @Test fun the_month_selector_shows_while_the_count_is_still_unknown() {
        assertTrue(SeeTransactionsUiState(month = august).isMonthSelectorVisible)
    }

    @Test fun the_month_selector_hides_once_a_filter_makes_the_list_cross_month() {
        val state = SeeTransactionsUiState(month = august, movementCount = 3, query = "café")

        assertFalse(state.isMonthSelectorVisible)
    }

    @Test fun a_count_known_to_be_zero_is_an_empty_ledger() {
        val state = SeeTransactionsUiState(month = august, movementCount = 0)

        assertEquals(ListDisplayState.EmptyLedger, state.listDisplayState)
    }

    @Test fun searching_an_empty_ledger_resolves_to_the_empty_ledger_alone() {
        // Previously hasNoTransactionsAtAll and hasNoResultsForFilter were both true here and only
        // the screen's `when` ordering broke the tie. A ledger with nothing in it cannot have
        // search results to miss, so the ledger state wins.
        val state = SeeTransactionsUiState(month = august, movementCount = 0, query = "café")

        assertEquals(ListDisplayState.EmptyLedger, state.listDisplayState)
    }

    @Test fun a_filter_that_matches_nothing_in_a_stocked_ledger_is_no_search_results() {
        val state = SeeTransactionsUiState(month = august, movementCount = 12, query = "café")

        assertEquals(ListDisplayState.NoSearchResults, state.listDisplayState)
    }

    @Test fun a_category_filter_alone_is_enough_to_reach_no_search_results() {
        val state = SeeTransactionsUiState(
            month = august,
            movementCount = 12,
            activeCategory = ActiveCategoryInfo(id = "cat-1", name = "Comida"),
        )

        assertEquals(ListDisplayState.NoSearchResults, state.listDisplayState)
    }

    @Test fun a_stocked_ledger_with_an_empty_month_and_no_filter_is_an_empty_month() {
        val state = SeeTransactionsUiState(month = august, movementCount = 12)

        assertEquals(ListDisplayState.EmptyMonth, state.listDisplayState)
    }

    @Test fun rows_render_as_content() {
        val state = SeeTransactionsUiState(month = august, movementCount = 12, days = listOf(day))

        assertEquals(ListDisplayState.Content, state.listDisplayState)
    }

    @Test fun rows_that_arrive_before_the_count_still_render_as_content() {
        val state = SeeTransactionsUiState(month = august, days = listOf(day))

        assertEquals(ListDisplayState.Content, state.listDisplayState)
    }
}
