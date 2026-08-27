package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiEffect

sealed interface LoanDetailEffect : UiEffect {
    data object NavigateToEditLoan : LoanDetailEffect
    data object LoanDeleted : LoanDetailEffect
    data class ShowError(val message: String) : LoanDetailEffect
}
