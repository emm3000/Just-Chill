package com.emm.justchill.hh.seetransactions

import com.emm.domain.category.CategoryType
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.recurring.PendingRecurringUi
import kotlinx.datetime.LocalDate

data class CategorySheetItem(
    val id: String,
    val name: String,
    val iconId: String,
    val colorId: String,
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
    /** Every category, most-used first — the filter sheet is the only way into a category filter. */
    val sheetItems: List<CategorySheetItem> = emptyList(),
    val incomeCount: Int = 0,
    val spendCount: Int = 0,
    val pendingRecurringMovements: List<PendingRecurringUi> = emptyList(),
    /** The clock's real month, refreshed each time [pendingRecurringMovements] re-emits. */
    val currentMonth: YearMonth = month,
    /** The clock's day, refreshed with [currentMonth]; `null` until that first emission. */
    val today: LocalDate? = null,
    /**
     * The id of the [PendingRecurringUi] the confirm sheet is open for, resolved from
     * [pendingRecurringMovements]; `null` means the sheet is closed (ADR 012 Decision 2).
     */
    val confirmSheetPendingId: String? = null,
    val showFilterSheet: Boolean = false,
    /**
     * Whether the search bar replaces the header, even before any character is typed — closing it
     * is [isSearchOpen]'s other half, alongside a non-blank [query] (ADR 012 Decision 2).
     */
    val searchRequested: Boolean = false,
) : UiState {

    val isFilterActive: Boolean
        get() = query.isNotBlank() || activeCategory != null

    val isSearchOpen: Boolean
        get() = searchRequested || query.isNotBlank()

    /**
     * A filter turns the list into a cross-month search, so the month selector steps aside; in
     * month mode it is always there, including before the ledger count is known.
     */
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

    /**
     * The nudge is about the day the user is living, so it shows only where that day would be:
     * the browsed month is the real one, nothing is filtering across months, and no group stands
     * for today already.
     */
    val isTodayNudgeVisible: Boolean
        get() = !isFilterActive && month == currentMonth && today != null && days.none { it.date == today }

    /**
     * Pending recurring movements are about "now": a filtered list stays filtered, and browsing a
     * past or future month must not surface today's pending row under a month it doesn't belong to.
     */
    val isPendingSectionVisible: Boolean
        get() = !isFilterActive && month == currentMonth && pendingRecurringMovements.isNotEmpty()
}
