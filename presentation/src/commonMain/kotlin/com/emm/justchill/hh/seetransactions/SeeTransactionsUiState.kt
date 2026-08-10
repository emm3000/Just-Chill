package com.emm.justchill.hh.seetransactions

import com.emm.domain.category.CategoryType
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.justchill.core.mvi.UiState

data class CategoryChipUi(val id: String, val name: String, val colorId: String, val selected: Boolean)

data class CategorySheetItem(
    val id: String,
    val name: String,
    val iconId: String,
    val colorId: String,
    val type: CategoryType,
    val isActive: Boolean,
)

data class ActiveCategoryInfo(val id: String, val name: String)

/** The selected month's totals, summed from its rows in the same pass that groups them. */
data class MonthSummaryUi(val income: Money, val spend: Money) {
    val net: Money
        get() = income - spend
}

/**
 * What the list area shows, resolved once and in one place.
 *
 * The states overlap on their raw conditions — searching a ledger that holds nothing is at the
 * same time "no ledger" and "no results" — so the precedence has to be decided somewhere. It is
 * decided here rather than in the ordering of a UI `when`, or the second UI to render this screen
 * would have to rediscover it.
 */
enum class ListDisplayState {
    /** The ledger count has not arrived yet: claim nothing, neither rows nor emptiness. */
    Loading,

    /** The count is known and it is zero — nothing has ever been recorded. */
    EmptyLedger,

    /** A filter is active over a ledger that does hold movements, and matched none of them. */
    NoSearchResults,

    /** Month mode over a ledger that holds movements, none of them in the selected month. */
    EmptyMonth,

    /** There are rows to draw. */
    Content,
}

data class SeeTransactionsUiState(
    val month: YearMonth = YearMonth.current(),
    val days: List<DayGroup> = emptyList(),
    val summary: MonthSummaryUi? = null,
    /**
     * Whole-ledger movement count, or null while it is still unknown. Nullable on purpose: a
     * `0L` default made every launch claim an empty ledger before the first aggregate emission.
     */
    val movementCount: Long? = null,
    val query: String = "",
    val topChips: List<CategoryChipUi> = emptyList(),
    val overflowCount: Int = 0,
    val activeCategory: ActiveCategoryInfo? = null,
    val sheetItems: List<CategorySheetItem> = emptyList(),
    val incomeCount: Int = 0,
    val spendCount: Int = 0,
) : UiState {

    val isFilterActive: Boolean
        get() = query.isNotBlank() || activeCategory != null

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
}
