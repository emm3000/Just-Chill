package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.presentation.format.balanceFormatted
import com.emm.justchill.core.presentation.format.normalizeForSearch
import com.emm.justchill.core.presentation.mvi.UiState

data class CategorySheetItem(
    val id: String,
    val name: String,
    val iconId: String,
    val type: CategoryType,
    val isActive: Boolean,
)

data class ActiveCategoryInfo(val id: String, val name: String)

data class MonthSummaryUi(val income: Money, val spend: Money) {
    val net: Money
        get() = income - spend
}

enum class FilterBannerSegmentKind {
    Plain,
    Emphasis,
    Query,
}

data class FilterBannerSegment(val text: String, val kind: FilterBannerSegmentKind)

enum class ListDisplayState {
    Loading,
    EmptyLedger,
    NoSearchResults,
    EmptyMonth,
    Content,
}

data class SeeTransactionsUiState(
    val month: YearMonth,
    val days: List<DayGroup> = emptyList(),
    val summary: MonthSummaryUi? = null,
    val movementCount: Long? = null,
    val query: String = "",
    val activeCategory: ActiveCategoryInfo? = null,
    val sheetItems: List<CategorySheetItem> = emptyList(),
    val incomeCount: Int = 0,
    val spendCount: Int = 0,
    val minAmount: Money? = null,
    val maxAmount: Money? = null,
    // null means the amount sheet is closed (ADR 012 Decision 2).
    val amountSheetTarget: AmountRangeTarget? = null,
    val currentMonth: YearMonth = month,
    val showFilterSheet: Boolean = false,
    val searchRequested: Boolean = false,
    val showMonthPicker: Boolean = false,
) : UiState {

    val minAmountCents: Long?
        get() = minAmount?.cents

    val maxAmountCents: Long?
        get() = maxAmount?.cents

    val isNetPositive: Boolean
        get() = (summary?.net?.cents ?: 0L) > 0L

    val isMonthYearVisible: Boolean
        get() = month.year != currentMonth.year

    val isCategoryOrAmountFilterActive: Boolean
        get() = activeCategory != null || minAmount != null || maxAmount != null

    val isFilterActive: Boolean
        get() = query.isNotBlank() || isCategoryOrAmountFilterActive

    val isSearchOpen: Boolean
        get() = searchRequested || query.isNotBlank()

    val listDisplayState: ListDisplayState
        get() = when {
            days.isNotEmpty() -> ListDisplayState.Content
            movementCount == null -> ListDisplayState.Loading
            movementCount == 0L -> ListDisplayState.EmptyLedger
            isFilterActive -> ListDisplayState.NoSearchResults
            else -> ListDisplayState.EmptyMonth
        }

    val isEyebrowVisible: Boolean
        get() = !isFilterActive && !isSearchOpen && listDisplayState != ListDisplayState.EmptyLedger

    val filterBannerSegments: List<FilterBannerSegment>
        get() {
            if (!isCategoryOrAmountFilterActive) return emptyList()
            val categoryName: String? = activeCategory?.name
            val range: String? = amountRangeText
            return buildList {
                if (categoryName != null) {
                    add(FilterBannerSegment("Filtrando por «", FilterBannerSegmentKind.Plain))
                    add(FilterBannerSegment(categoryName, FilterBannerSegmentKind.Emphasis))
                    add(FilterBannerSegment("»", FilterBannerSegmentKind.Plain))
                }
                if (range != null) {
                    if (categoryName != null) add(FilterBannerSegment(", ", FilterBannerSegmentKind.Plain))
                    add(FilterBannerSegment(range, FilterBannerSegmentKind.Emphasis))
                }
                if (query.isNotBlank()) {
                    add(FilterBannerSegment(" + \"", FilterBannerSegmentKind.Plain))
                    add(FilterBannerSegment(query, FilterBannerSegmentKind.Query))
                    add(FilterBannerSegment("\"", FilterBannerSegmentKind.Plain))
                }
            }
        }

    fun sheetItemsMatching(segment: CategoryType, query: String): List<CategorySheetItem> {
        val needle: String = query.normalizeForSearch()
        return sheetItems.filter { it.type == segment && it.name.normalizeForSearch().contains(needle) }
    }

    private val amountRangeText: String?
        get() {
            val minimum: String? = minAmount?.balanceFormatted()
            val maximum: String? = maxAmount?.balanceFormatted()
            return when {
                minimum != null && maximum != null -> "$minimum – $maximum"
                minimum != null -> "desde $minimum"
                maximum != null -> "hasta $maximum"
                else -> null
            }
        }
}
