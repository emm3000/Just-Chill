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

/**
 * Mutually exclusive in-flight operation.
 *
 * Operations are serialized by design: e.g. you cannot import while an export
 * is in progress. [isSyncing] and [syncRow] are orchestrator-driven and remain
 * separate — they are NOT gated by this enum.
 *
 * [BackingUp] is the one value no `launchOp` call sets. The backup pipeline runs outside this
 * ViewModel, so the flag is mirrored in from `BackupController.isBackingUp` and claims this slot
 * only while it is free — see `ProfileViewModel.onBackupProgress` for why it cannot just overwrite.
 */
enum class ProfileOp { None, Exporting, Importing, DeletingAccount, SigningOut, BackingUp }

data class ProfileUiState(
    val op: ProfileOp = ProfileOp.None,
    val isSyncing: Boolean = false,
    val syncRow: SyncRowUi = SyncRowUi.Idle(null),
    val categoryCount: Int = 0,
    val accountCount: Int = 0,
    val session: SessionUiState = SessionUiState.Initializing,
) : UiState
