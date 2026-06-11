package com.emm.justchill.hh.profile

import com.emm.justchill.core.mvi.UiState

sealed interface SessionUiState {
    data object Initializing : SessionUiState
    data object SignedOut : SessionUiState
    data class SignedIn(val email: String?) : SessionUiState
}

data class ProfileUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isSyncing: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val lastSyncedAtMillis: Long? = null,
    val syncFailed: Boolean = false,
    val categoryCount: Int = 0,
    val accountCount: Int = 0,
    val session: SessionUiState = SessionUiState.Initializing,
) : UiState
