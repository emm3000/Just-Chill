package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.mvi.UiState

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
    // Every category, most-used first — the filter sheet is the only way into a category filter.
    val sheetItems: List<CategorySheetItem> = emptyList(),
    val incomeCount: Int = 0,
    val spendCount: Int = 0,
    val minAmount: Money? = null,
    val maxAmount: Money? = null,
    // null means the amount sheet is closed (ADR 012 Decision 2).
    val amountSheetTarget: AmountRangeTarget? = null,
    val currentMonth: YearMonth = month,
    val showFilterSheet: Boolean = false,
    // Closing this is isSearchOpen's other half, alongside a non-blank query (ADR 012 Decision 2).
    val searchRequested: Boolean = false,
    val showMonthPicker: Boolean = false,
) : UiState {

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

    // An eyebrow naming a spend that does not exist says nothing (PRD §3, ADR 022).
    val isEyebrowVisible: Boolean
        get() = !isFilterActive && !isSearchOpen && listDisplayState != ListDisplayState.EmptyLedger
}
