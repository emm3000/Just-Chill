package com.emm.justchill.hh.recurring

import com.emm.justchill.core.mvi.UiIntent

sealed interface RecurringMovementsIntent : UiIntent {
    data object NavigateToAdd : RecurringMovementsIntent
    data class NavigateToEdit(val id: String) : RecurringMovementsIntent
    data class RequestDelete(val id: String) : RecurringMovementsIntent
    data object ConfirmDelete : RecurringMovementsIntent
    data object DismissDelete : RecurringMovementsIntent
}
