package com.emm.justchill.hh.seetransactions

import androidx.compose.runtime.Immutable
import com.emm.domain.category.CategoryType
import com.emm.justchill.core.mvi.UiState

@Immutable
data class CategoryChipUi(val id: String, val name: String, val colorId: String, val selected: Boolean)

@Immutable
data class CategorySheetItem(
    val id: String,
    val name: String,
    val iconId: String,
    val colorId: String,
    val type: CategoryType,
    val isActive: Boolean,
)

@Immutable
data class ActiveCategoryInfo(val id: String, val name: String)

data class SeeTransactionsUiState(
    val days: List<DayGroup> = emptyList(),
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

    val hasNoTransactionsAtAll: Boolean
        get() = days.isEmpty() && !isFilterActive

    val hasNoResultsForFilter: Boolean
        get() = days.isEmpty() && isFilterActive
}
