package com.emm.justchill.feature.loan

import com.emm.justchill.core.ui.format.relativeDayLabel
import com.emm.justchill.core.ui.mvi.UiState
import kotlinx.datetime.LocalDate

data class AddEditLoanUiState(
    val today: LocalDate,
    val isEdit: Boolean = false,
    val personName: String = "",
    val personSuggestions: List<String> = emptyList(),
    val amountDigits: String = "",
    val interestPercentText: String = "",
    val date: LocalDate? = null,
    val note: String = "",
    val isSaveEnabled: Boolean = false,
    val isSaving: Boolean = false,
    // null means no sheet is open (ADR 012 Decision 2).
    val openSheet: LoanFormSheet? = null,
) : UiState {
    val dateLabel: String get() = relativeDayLabel(pickerDate, today)
    val pickerDate: LocalDate get() = date ?: today
}
