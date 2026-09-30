package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.presentation.format.CURRENCY_PREFIX
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeeTransactionsUiStateTest {

    private val august = YearMonth(2026, Month.AUGUST)
    private val food: ActiveCategoryInfo = ActiveCategoryInfo(id = "cat-1", name = "Comida")
    private val closedRange: String = "${CURRENCY_PREFIX}20.00 – ${CURRENCY_PREFIX}50.00"

    private val day = DayGroup(
        date = LocalDate(2026, 8, 10),
        today = LocalDate(2026, 8, 10),
        transactions = emptyList(),
    )

    @Test fun unknown_count_is_loading_not_an_empty_state() {
        val state = SeeTransactionsUiState(month = august)

        assertEquals(ListDisplayState.Loading, state.listDisplayState)
    }

    @Test fun the_eyebrow_shows_while_the_count_is_still_unknown() {
        assertTrue(SeeTransactionsUiState(month = august).isEyebrowVisible)
    }

    @Test fun the_eyebrow_hides_on_an_empty_ledger() {
        val state = SeeTransactionsUiState(month = august, movementCount = 0)

        assertFalse(state.isEyebrowVisible)
    }

    @Test fun the_eyebrow_shows_on_an_empty_month() {
        val state = SeeTransactionsUiState(month = august, movementCount = 12)

        assertTrue(state.isEyebrowVisible)
    }

    @Test fun the_eyebrow_shows_over_content() {
        val state = SeeTransactionsUiState(month = august, movementCount = 12, days = listOf(day))

        assertTrue(state.isEyebrowVisible)
    }

    @Test fun the_eyebrow_hides_once_a_category_filter_makes_the_list_cross_month() {
        val state = SeeTransactionsUiState(
            month = august,
            movementCount = 3,
            activeCategory = food,
        )

        assertFalse(state.isEyebrowVisible)
    }

    @Test fun the_eyebrow_hides_once_an_amount_bound_makes_the_list_cross_month() {
        val state = SeeTransactionsUiState(
            month = august,
            movementCount = 3,
            minAmount = Money(1000),
        )

        assertFalse(state.isEyebrowVisible)
    }

    @Test fun the_eyebrow_hides_while_search_is_open() {
        val state = SeeTransactionsUiState(month = august, movementCount = 3, searchRequested = true)

        assertFalse(state.isEyebrowVisible)
    }

    @Test fun a_count_known_to_be_zero_is_an_empty_ledger() {
        val state = SeeTransactionsUiState(month = august, movementCount = 0)

        assertEquals(ListDisplayState.EmptyLedger, state.listDisplayState)
    }

    @Test fun searching_an_empty_ledger_resolves_to_the_empty_ledger_alone() {
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
            activeCategory = food,
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

    @Test fun the_month_picker_sheet_is_closed_by_default() {
        assertFalse(SeeTransactionsUiState(month = august).showMonthPicker)
    }

    @Test fun `amount bounds also read as whole cents`() {
        val state = SeeTransactionsUiState(month = august, minAmount = Money(1_500L), maxAmount = Money(9_900L))

        assertEquals(1_500L, state.minAmountCents)
        assertEquals(9_900L, state.maxAmountCents)
    }

    @Test fun `an absent amount bound has no cents`() {
        val state = SeeTransactionsUiState(month = august)

        assertNull(state.minAmountCents)
        assertNull(state.maxAmountCents)
    }

    @Test fun `the net is positive only while income exceeds spend`() {
        val ahead = SeeTransactionsUiState(
            month = august,
            summary = MonthSummaryUi(income = Money(1_000L), spend = Money(400L)),
        )
        val even = SeeTransactionsUiState(
            month = august,
            summary = MonthSummaryUi(income = Money(400L), spend = Money(400L)),
        )
        val behind = SeeTransactionsUiState(
            month = august,
            summary = MonthSummaryUi(income = Money(100L), spend = Money(400L)),
        )

        assertTrue(ahead.isNetPositive)
        assertFalse(even.isNetPositive)
        assertFalse(behind.isNetPositive)
    }

    @Test fun `a month without a summary has no positive net`() {
        assertFalse(SeeTransactionsUiState(month = august).isNetPositive)
    }

    @Test fun `the month year shows only outside the current year`() {
        val earlierYear = SeeTransactionsUiState(month = YearMonth(2025, Month.DECEMBER), currentMonth = august)
        val sameYear = SeeTransactionsUiState(month = YearMonth(2026, Month.JANUARY), currentMonth = august)

        assertTrue(earlierYear.isMonthYearVisible)
        assertFalse(sameYear.isMonthYearVisible)
    }

    @Test
    fun `no category and no range leave the banner empty even with a query`() {
        val state: SeeTransactionsUiState = SeeTransactionsUiState(month = august, query = "pan")

        assertEquals(emptyList(), state.filterBannerSegments)
    }

    @Test
    fun `a category alone emphasises its name inside the guillemets`() {
        val state: SeeTransactionsUiState = SeeTransactionsUiState(month = august, activeCategory = food)

        assertEquals(
            listOf(
                FilterBannerSegment("Filtrando por «", FilterBannerSegmentKind.Plain),
                FilterBannerSegment("Comida", FilterBannerSegmentKind.Emphasis),
                FilterBannerSegment("»", FilterBannerSegmentKind.Plain),
            ),
            state.filterBannerSegments,
        )
    }

    @Test
    fun `a closed range emphasises both bounds joined by an en dash`() {
        val state: SeeTransactionsUiState = SeeTransactionsUiState(
            month = august,
            minAmount = Money(2_000L),
            maxAmount = Money(5_000L),
        )

        assertEquals(
            listOf(
                FilterBannerSegment(closedRange, FilterBannerSegmentKind.Emphasis),
            ),
            state.filterBannerSegments,
        )
    }

    @Test
    fun `a lower bound alone reads desde`() {
        val state: SeeTransactionsUiState = SeeTransactionsUiState(month = august, minAmount = Money(2_000L))

        assertEquals(
            listOf(FilterBannerSegment("desde ${CURRENCY_PREFIX}20.00", FilterBannerSegmentKind.Emphasis)),
            state.filterBannerSegments,
        )
    }

    @Test
    fun `an upper bound alone reads hasta`() {
        val state: SeeTransactionsUiState = SeeTransactionsUiState(month = august, maxAmount = Money(5_000L))

        assertEquals(
            listOf(FilterBannerSegment("hasta ${CURRENCY_PREFIX}50.00", FilterBannerSegmentKind.Emphasis)),
            state.filterBannerSegments,
        )
    }

    @Test
    fun `a category, a range and a query read in that order with the query quoted`() {
        val state: SeeTransactionsUiState = SeeTransactionsUiState(
            month = august,
            activeCategory = food,
            minAmount = Money(2_000L),
            maxAmount = Money(5_000L),
            query = "pan",
        )

        assertEquals(
            listOf(
                FilterBannerSegment("Filtrando por «", FilterBannerSegmentKind.Plain),
                FilterBannerSegment("Comida", FilterBannerSegmentKind.Emphasis),
                FilterBannerSegment("»", FilterBannerSegmentKind.Plain),
                FilterBannerSegment(", ", FilterBannerSegmentKind.Plain),
                FilterBannerSegment(closedRange, FilterBannerSegmentKind.Emphasis),
                FilterBannerSegment(" + \"", FilterBannerSegmentKind.Plain),
                FilterBannerSegment("pan", FilterBannerSegmentKind.Query),
                FilterBannerSegment("\"", FilterBannerSegmentKind.Plain),
            ),
            state.filterBannerSegments,
        )
    }

    @Test
    fun `a blank query adds nothing to the banner`() {
        val state: SeeTransactionsUiState = SeeTransactionsUiState(month = august, activeCategory = food, query = "  ")

        assertEquals(3, state.filterBannerSegments.size)
    }
}
