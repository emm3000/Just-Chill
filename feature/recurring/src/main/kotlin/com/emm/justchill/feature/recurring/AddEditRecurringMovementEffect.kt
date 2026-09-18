package com.emm.justchill.feature.recurring

import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface AddEditRecurringMovementEffect : UiEffect {
    data object NavigateBack : AddEditRecurringMovementEffect
    data class ShowError(val message: String) : AddEditRecurringMovementEffect
}
