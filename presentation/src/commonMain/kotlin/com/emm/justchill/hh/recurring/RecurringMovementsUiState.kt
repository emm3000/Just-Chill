package com.emm.justchill.hh.recurring

import com.emm.justchill.core.mvi.UiState

data class RecurringMovementsUiState(
    val activeItems: List<RecurringMovementUi> = emptyList(),
    val pausedItems: List<RecurringMovementUi> = emptyList(),
    val entranFormatted: String = "S/ 0.00",
    val salenFormatted: String = "S/ 0.00",
    val variableCount: Int = 0,
    val pendingDelete: String? = null,
) : UiState
