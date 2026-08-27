package com.emm.justchill.hh.seetransactions

import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.justchill.core.mvi.UiIntent

sealed interface SeeTransactionsIntent : UiIntent {
    data object OnPreviousMonth : SeeTransactionsIntent
    data object OnNextMonth : SeeTransactionsIntent
    data class OnQueryChanged(val query: String) : SeeTransactionsIntent
    data class OnCategoryToggled(val categoryId: String) : SeeTransactionsIntent
    data class OnCategorySelected(val categoryId: String) : SeeTransactionsIntent
    data object OnClearCategoryFilter : SeeTransactionsIntent
    data object OnClearFilters : SeeTransactionsIntent

    data class ConfirmRecurring(val templateId: String, val period: YearMonth, val callerAmount: Money?) :
        SeeTransactionsIntent

    /** Settles [period] with no transaction — the month the user genuinely did not pay. */
    data class SkipRecurring(val templateId: String, val period: YearMonth) : SeeTransactionsIntent

    /** Opens the confirm sheet for the tapped pending row. */
    data class OnPendingClicked(val pendingId: String) : SeeTransactionsIntent

    /** The user dismissed the confirm sheet without confirming or skipping. */
    data object OnConfirmSheetDismissed : SeeTransactionsIntent
}
