package com.emm.justchill.hh.loan

import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface AddEditLoanEffect : UiEffect {
    data object NavigateBack : AddEditLoanEffect
    data class ShowError(val message: String) : AddEditLoanEffect
}
