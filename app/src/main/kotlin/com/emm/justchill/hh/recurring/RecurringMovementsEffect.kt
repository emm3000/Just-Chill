package com.emm.justchill.hh.recurring

import com.emm.justchill.core.mvi.UiEffect

sealed interface RecurringMovementsEffect : UiEffect {
    data class NavigateToAddEdit(val id: String? = null) : RecurringMovementsEffect
    data class ShowError(val message: String) : RecurringMovementsEffect
}
