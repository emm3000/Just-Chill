package com.emm.justchill.feature.loan

import com.emm.justchill.core.ui.mvi.UiIntent
import kotlinx.datetime.LocalDate

sealed interface AddEditLoanIntent : UiIntent {
    data class OnPersonNameChange(val value: String) : AddEditLoanIntent
    data class OnAmountChange(val digits: String) : AddEditLoanIntent
    data class OnInterestPercentChange(val value: String) : AddEditLoanIntent
    data class OnDateSelected(val value: LocalDate) : AddEditLoanIntent
    data class OnNoteChange(val value: String) : AddEditLoanIntent
    data object Save : AddEditLoanIntent

    data class OnSheetRequested(val sheet: LoanFormSheet) : AddEditLoanIntent

    data object OnSheetDismissed : AddEditLoanIntent
}
