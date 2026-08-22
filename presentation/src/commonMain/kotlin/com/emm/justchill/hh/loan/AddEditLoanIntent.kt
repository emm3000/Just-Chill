package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiIntent
import kotlinx.datetime.LocalDate

sealed interface AddEditLoanIntent : UiIntent {
    data class OnPersonNameChange(val value: String) : AddEditLoanIntent
    data class OnPersonSuggestionSelected(val value: String) : AddEditLoanIntent
    data class OnAmountChange(val digits: String) : AddEditLoanIntent
    data class OnInterestPercentChange(val value: String) : AddEditLoanIntent
    data class OnDateSelected(val value: LocalDate) : AddEditLoanIntent
    data class OnNoteChange(val value: String) : AddEditLoanIntent
    data object Save : AddEditLoanIntent
}
