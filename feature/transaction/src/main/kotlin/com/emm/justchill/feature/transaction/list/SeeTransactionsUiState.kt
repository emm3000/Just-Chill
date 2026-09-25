package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.mvi.UiState
import com.emm.justchill.core.ui.pending.PendingRecurringUi
import kotlinx.datetime.LocalDate

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
    val pendingRecurringMovements: List<PendingRecurringUi> = emptyList(),
    // The clock's real month, refreshed each time pendingRecurringMovements re-emits.
    val currentMonth: YearMonth = month,
    // The clock's day, straight off TodayFlow; null before its first emission.
    val today: LocalDate? = null,
    // null means the confirm sheet is closed (ADR 012 Decision 2).
    val confirmSheetPendingId: String? = null,
    val showFilterSheet: Boolean = false,
    // Closing this is isSearchOpen's other half, alongside a non-blank query (ADR 012 Decision 2).
    val searchRequested: Boolean = false,
) : UiState {

    // The sheet-governed half of the filter: category and amount range, never the free-text query.
    // Drives the header badge, the banner's visibility and the sheet's "Limpiar filtro".
    val isCategoryOrAmountFilterActive: Boolean
        get() = activeCategory != null || minAmount != null || maxAmount != null

    val isFilterActive: Boolean
        get() = query.isNotBlank() || isCategoryOrAmountFilterActive

    val isSearchOpen: Boolean
        get() = searchRequested || query.isNotBlank()

    // A filter turns the list into a cross-month search, so the month selector steps aside; in
    // month mode it is always there, including before the ledger count is known.
    val isMonthSelectorVisible: Boolean
        get() = !isFilterActive

    val listDisplayState: ListDisplayState
        get() = when {
            days.isNotEmpty() -> ListDisplayState.Content
            movementCount == null -> ListDisplayState.Loading
            movementCount == 0L -> ListDisplayState.EmptyLedger
            isFilterActive -> ListDisplayState.NoSearchResults
            else -> ListDisplayState.EmptyMonth
        }

    // ListDisplayState.EmptyLedger already fills the screen with the same invitation.
    val isTodayNudgeVisible: Boolean
        get() = !isFilterActive &&
            month == currentMonth &&
            today != null &&
            listDisplayState != ListDisplayState.EmptyLedger &&
            days.none { it.date == today }

    // Pending recurring movements are about "now": a filtered list stays filtered, and browsing a
    // past or future month must not surface today's pending row under a month it doesn't belong to.
    val isPendingSectionVisible: Boolean
        get() = !isFilterActive && month == currentMonth && pendingRecurringMovements.isNotEmpty()
}
