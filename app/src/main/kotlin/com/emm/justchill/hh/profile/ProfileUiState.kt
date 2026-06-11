package com.emm.justchill.hh.profile

import com.emm.justchill.core.mvi.UiState

sealed interface SessionUiState {
    data object Initializing : SessionUiState
    data object SignedOut : SessionUiState
    data class SignedIn(val email: String?) : SessionUiState
}

/**
 * Presentation state for the sync row in the signed-in account section.
 *
 * The precedence rule (Syncing wins over Failed) lives here in one place —
 * not scattered across the composable.
 */
sealed interface SyncRowUi {
    data object Syncing : SyncRowUi
    data object Failed : SyncRowUi
    data class Idle(val lastSyncedAtMillis: Long?) : SyncRowUi
}

data class ProfileUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isSyncing: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val syncRow: SyncRowUi = SyncRowUi.Idle(null),
    val categoryCount: Int = 0,
    val accountCount: Int = 0,
    val session: SessionUiState = SessionUiState.Initializing,
) : UiState
