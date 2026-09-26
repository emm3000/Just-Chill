package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.mvi.UiIntent

sealed interface SeeTransactionsIntent : UiIntent {
    data class OnMonthSelected(val month: YearMonth) : SeeTransactionsIntent
    data class OnQueryChanged(val query: String) : SeeTransactionsIntent
    data class OnCategoryToggled(val categoryId: String) : SeeTransactionsIntent
    data class OnCategorySelected(val categoryId: String) : SeeTransactionsIntent
    data object OnClearCategoryFilter : SeeTransactionsIntent
    data object OnClearFilters : SeeTransactionsIntent

    // Grouped for the same CyclomaticComplexMethod reason as LoanDetailIntent.PaymentFormIntent.
    sealed interface ScreenChromeIntent : SeeTransactionsIntent {
        data object OnFilterSheetRequested : ScreenChromeIntent
        data object OnFilterSheetDismissed : ScreenChromeIntent
        data object OnSearchRequested : ScreenChromeIntent
        data object OnSearchClosed : ScreenChromeIntent
        data object OnMonthPickerRequested : ScreenChromeIntent
        data object OnMonthPickerDismissed : ScreenChromeIntent
    }

    sealed interface AmountFilterIntent : SeeTransactionsIntent {
        data class OnAmountSheetRequested(val target: AmountRangeTarget) : AmountFilterIntent
        data object OnAmountSheetDismissed : AmountFilterIntent
        data class OnAmountConfirmed(val digits: String) : AmountFilterIntent
        data class OnAmountBoundCleared(val target: AmountRangeTarget) : AmountFilterIntent
    }
}
