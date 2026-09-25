package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.mvi.UiIntent

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

    // Settles period with no transaction — the month the user genuinely did not pay.
    data class SkipRecurring(val templateId: String, val period: YearMonth) : SeeTransactionsIntent

    data class OnPendingClicked(val pendingId: String) : SeeTransactionsIntent

    data object OnConfirmSheetDismissed : SeeTransactionsIntent

    // Grouped for the same CyclomaticComplexMethod reason as LoanDetailIntent.PaymentFormIntent.
    sealed interface ScreenChromeIntent : SeeTransactionsIntent {
        data object OnFilterSheetRequested : ScreenChromeIntent
        data object OnFilterSheetDismissed : ScreenChromeIntent
        data object OnSearchRequested : ScreenChromeIntent
        data object OnSearchClosed : ScreenChromeIntent
    }

    sealed interface AmountFilterIntent : SeeTransactionsIntent {
        data class OnAmountSheetRequested(val target: AmountRangeTarget) : AmountFilterIntent
        data object OnAmountSheetDismissed : AmountFilterIntent
        data class OnAmountConfirmed(val digits: String) : AmountFilterIntent
        data class OnAmountBoundCleared(val target: AmountRangeTarget) : AmountFilterIntent
    }
}
