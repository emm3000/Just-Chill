package com.emm.justchill.hh.recurring

import androidx.compose.runtime.Stable
import com.emm.justchill.core.mvi.UiState

@Stable
data class RecurringMovementsUiState(
    val items: List<RecurringMovementUi> = emptyList(),
    val pendingDelete: String? = null,
) : UiState
