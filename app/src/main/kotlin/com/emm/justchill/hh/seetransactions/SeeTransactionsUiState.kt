package com.emm.justchill.hh.seetransactions

import androidx.compose.runtime.Immutable
import com.emm.justchill.core.mvi.UiState

@Immutable
data class CategoryChipUi(
    val id: String,
    val name: String,
    val colorId: String,
    val selected: Boolean,
)

data class SeeTransactionsUiState(
    val days: List<DayGroup> = emptyList(),
    val query: String = "",
    val categoryChips: List<CategoryChipUi> = emptyList(),
    val isFilterActive: Boolean = false,
) : UiState {

    val hasNoTransactionsAtAll: Boolean
        get() = days.isEmpty() && !isFilterActive

    val hasNoResultsForFilter: Boolean
        get() = days.isEmpty() && isFilterActive
}
