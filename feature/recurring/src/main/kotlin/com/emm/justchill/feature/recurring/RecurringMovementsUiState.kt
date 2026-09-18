package com.emm.justchill.feature.recurring

import com.emm.justchill.core.ui.mvi.UiState

data class RecurringMovementsUiState(
    val activeItems: List<RecurringMovementUi> = emptyList(),
    val pausedItems: List<RecurringMovementUi> = emptyList(),
    val entranFormatted: String = "S/ 0.00",
    val salenFormatted: String = "S/ 0.00",
    val variableCount: Int = 0,
    val pendingDelete: String? = null,
) : UiState
