package com.emm.justchill.feature.loan

import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface LoanDetailEffect : UiEffect {
    data object NavigateToEditLoan : LoanDetailEffect
    data object LoanDeleted : LoanDetailEffect
    data class ShowError(val message: String) : LoanDetailEffect
}
