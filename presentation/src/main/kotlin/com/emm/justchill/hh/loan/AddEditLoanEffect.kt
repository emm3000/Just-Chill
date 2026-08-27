package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiEffect

sealed interface AddEditLoanEffect : UiEffect {
    data object NavigateBack : AddEditLoanEffect
    data class ShowError(val message: String) : AddEditLoanEffect
}
