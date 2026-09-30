package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.presentation.format.CURRENCY_PREFIX
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class SeeTransactionsFilterBannerTest {

    private val august: YearMonth = YearMonth(2026, Month.AUGUST)
    private val closedRange: String = "${CURRENCY_PREFIX}20.00 – ${CURRENCY_PREFIX}50.00"
    private val food: ActiveCategoryInfo = ActiveCategoryInfo(id = "food", name = "Comida")

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
                FilterBannerSegment(
                    "${CURRENCY_PREFIX}20.00 – ${CURRENCY_PREFIX}50.00",
                    FilterBannerSegmentKind.Emphasis,
                ),
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
                FilterBannerSegment(
                    "${CURRENCY_PREFIX}20.00 – ${CURRENCY_PREFIX}50.00",
                    FilterBannerSegmentKind.Emphasis,
                ),
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
