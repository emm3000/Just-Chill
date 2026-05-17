package com.emm.justchill.hh.seetransactions

import com.emm.justchill.core.mvi.UiIntent

sealed interface SeeTransactionsIntent : UiIntent {
    data class OnQueryChanged(val query: String) : SeeTransactionsIntent
    data class OnCategoryToggled(val categoryId: String) : SeeTransactionsIntent
    data object OnClearFilters : SeeTransactionsIntent
}
