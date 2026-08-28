package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty
import com.emm.justchill.hh.shared.relativeDayLabel
import kotlinx.datetime.LocalDate

data class AddEditLoanUiState(
    val today: LocalDate,
    val isEdit: Boolean = false,
    val personName: String = String.Empty,
    val personSuggestions: List<String> = emptyList(),
    val amountDigits: String = String.Empty,
    val interestPercentText: String = String.Empty,
    val date: LocalDate? = null,
    val note: String = String.Empty,
    val isSaveEnabled: Boolean = false,
    val isSaving: Boolean = false,
    /** The sheet currently rendered over this screen; `null` means none is (ADR 012 Decision 2). */
    val openSheet: LoanFormSheet? = null,
) : UiState {
    val dateLabel: String get() = relativeDayLabel(pickerDate, today)
    val pickerDate: LocalDate get() = date ?: today
}
