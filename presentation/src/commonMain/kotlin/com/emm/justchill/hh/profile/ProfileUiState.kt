package com.emm.justchill.hh.profile

import com.emm.domain.shared.backup.BackupFailureReason
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
 * Presentation state for the "Último respaldo" row in the Respaldo section.
 *
 * Same principle as [SyncRowUi] above, and the same reason: the discrimination is resolved here, in
 * one place, **not scattered across the composable**. `BackupHealth` deliberately publishes three
 * different facts through one nullable `lastSuccessfulBackupAt` — no session, orchestrator not
 * started, signed in but never backed up — and the ViewModel is the only place that holds the
 * session, the health and the staleness answer at once. A composable rebuilding that from an
 * `if (x == null)` chain would be guessing at two of the three.
 *
 * ### Precedence, in the order the `when` applies it
 *
 * [NeedsAccount] > [BackingUp] > [Unreadable] > [Failed] > [Never] > [Stale] > [UpToDate], with the
 * reasoning on `ProfileViewModel.resolveBackupRow`. Two positions are load-bearing rather than
 * arbitrary: [NeedsAccount] outranks [BackingUp] so a cycle still in flight when the session ends
 * cannot render "Respaldando…" to a signed-out user, and **a failure does not outrank the snapshot
 * itself** — [Failed] carries the age instead, so one bad manual tap cannot hide a backup that
 * succeeded this morning.
 *
 * ### The one trap, spelled out in `BackupHealth`'s KDoc
 *
 * [Failed] is chosen on `consecutiveFailures > 0` and **never** on `lastFailureReason != null`.
 * `BackupFailureReason.fromNameOrNull` answers null for a persisted reason name this build no longer
 * has, so `(5, null)` is a real, reachable value; keying on the reason would hide a device that has
 * failed five times running. The reason only refines the wording, which is why [Failed] carries it
 * as a nullable and the copy has a fallback.
 */
sealed interface BackupRowUi {

    /**
     * A cycle is running right now, automatic ones included.
     *
     * Not redundant with `op == ProfileOp.BackingUp`, which the "Respaldar ahora" row reads: that
     * flag claims the shared op slot only while it is free, so an automatic backup landing mid-export
     * is invisible there by design. This one comes straight off `BackupController.isBackingUp` and is
     * therefore the honest of the two.
     */
    data object BackingUp : BackupRowUi

    /**
     * There is nobody to back up for — signed out, or the session has not resolved yet.
     *
     * Both cases fold together deliberately: `AccountSection` already renders its signed-out row for
     * `SessionUiState.Initializing`, and a fourth transient state on the same screen would only be
     * visible for the milliseconds the session takes to arrive.
     */
    data object NeedsAccount : BackupRowUi

    /** Signed in, nothing has failed, and this device has never completed a snapshot for this account. */
    data object Never : BackupRowUi

    /**
     * The row could not be built: reading how old the last snapshot is threw.
     *
     * Distinct from [Failed] because the two are different sentences. [Failed] says a backup *cycle*
     * did not produce a snapshot — a fact the health surface persisted. This says the app cannot
     * currently describe the snapshot it has, which claims nothing about whether backup is working.
     * Collapsing it into `Failed(reason, lastBackupDaysAgo = null)` would render as "Sin respaldo" on
     * a device that has one.
     */
    data object Unreadable : BackupRowUi

    /**
     * At least one cycle has failed since the last verified snapshot — **and what that snapshot is**.
     *
     * @property reason may be null; read the trap note above before keying anything on it.
     * @property lastBackupDaysAgo the age of the last verified snapshot, or null when there is none.
     *   It is here so a failure cannot erase a good backup from the row: a manual tap on bad wifi an
     *   hour after a successful automatic cycle is an ordinary sequence, and the daily cap then keeps
     *   the streak from clearing until midnight. `null` is the case that is genuinely alarming —
     *   failing with nothing backed up at all — and it is the one the copy shouts about.
     */
    data class Failed(val reason: BackupFailureReason?, val lastBackupDaysAgo: Int?) : BackupRowUi

    /** A verified snapshot exists, but it is older than the threshold **and** the ledger has moved since. */
    data class Stale(val daysSinceLastBackup: Int) : BackupRowUi

    /** A verified snapshot exists and nothing above applies. */
    data class UpToDate(val daysSinceLastBackup: Int) : BackupRowUi
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
    // Starts on NeedsAccount for the same reason `session` starts on Initializing: nothing has been
    // observed yet, and the two agree — BackupRowUi.NeedsAccount is what an unresolved session maps to.
    val backupRow: BackupRowUi = BackupRowUi.NeedsAccount,
) : UiState
