package com.emm.justchill.hh.recurring

import com.emm.justchill.core.mvi.UiState

data class RecurringMovementsUiState(
    /** Active templates (isActive == true), sorted by name ASC. */
    val activeItems: List<RecurringMovementUi> = emptyList(),
    /** Paused templates (isActive == false), sorted by name ASC. */
    val pausedItems: List<RecurringMovementUi> = emptyList(),
    /** Pre-formatted income total with currency symbol, no sign — e.g. "S/ 4,500.00". */
    val entranFormatted: String = "S/ 0.00",
    /** Pre-formatted expense total with currency symbol, no sign — e.g. "S/ 1,800.00". */
    val salenFormatted: String = "S/ 0.00",
    /** Count of active templates with variable (null) amount — drives the footnote. */
    val variableCount: Int = 0,
    /** ID of the template awaiting delete confirmation; null when no dialog is open. */
    val pendingDelete: String? = null,
) : UiState
