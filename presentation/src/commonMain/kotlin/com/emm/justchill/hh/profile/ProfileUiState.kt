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
 * What this device's last verified snapshot is, for the row that has to describe it.
 *
 * Three cases, because there genuinely are three and the previous `Int?` could only carry two. It
 * replaced that nullable after review pointed out the same ambiguity [BackupRowUi.Unreadable] exists
 * to remove: `null` meant "no snapshot" and the copy shouted "Sin respaldo", but nothing in the type
 * stopped it from describing a device that had one whose age simply could not be read. The invariant
 * held only because one function constructed it. Now it cannot be stated wrongly at all.
 */
sealed interface LastSnapshot {

    /** This device has never completed a verified snapshot for this account. */
    data object None : LastSnapshot

    /**
     * A snapshot exists, but reading how old it is threw.
     *
     * Never says "Sin respaldo": there is one, and the row simply cannot date it.
     */
    data object AgeUnknown : LastSnapshot

    /**
     * A snapshot from [days] calendar days ago, and whether the staleness rule calls it stale.
     *
     * [isStale] is carried rather than recomputed because it is not the age alone — it is the age
     * **and** the ledger having moved since (`GetBackupStalenessUseCase`). A month-old snapshot of a
     * ledger nobody has touched is complete, and must not be escalated as if it were not.
     */
    data class DaysAgo(val days: Int, val isStale: Boolean) : LastSnapshot
}

/**
 * How loudly the row should be drawn. Semantic, not a colour — `:presentation` cannot know what a
 * `Color` is, and each platform maps these onto its own tokens.
 */
enum class BackupRowSeverity { Normal, Warning, Danger }

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
 * ### The shape is two-level, not one chain
 *
 * An earlier version of this KDoc — and of `ProfileViewModel`'s, and of `docs/sync/ADR009_PLAN.md` —
 * claimed a single precedence chain `NeedsAccount > BackingUp > Unreadable > Failed > Never > Stale
 * > UpToDate`. **The code has never had that order**, and three copies of a wrong contract are three
 * chances to be believed. What it actually does:
 *
 * ```
 * resolveBackupRow          not signed in ............... NeedsAccount
 *                           a cycle is running .......... BackingUp
 *                           no watermark ................ failing ? Failed(reason, None) : Never
 *                           a watermark ................. ↓
 * snapshotRow               the staleness read threw .... failing ? Failed(reason, AgeUnknown)
 *                                                                 : Unreadable
 *                           otherwise ................... failing ? Failed(reason, DaysAgo(…))
 *                                                                 : isStale ? Stale : UpToDate
 * ```
 *
 * Two positions are load-bearing rather than arbitrary. [NeedsAccount] outranks [BackingUp] so a
 * cycle still in flight when the session ends cannot render "Respaldando…" to a signed-out user. And
 * **a failure never replaces the snapshot; it annotates it** — [Failed] carries [LastSnapshot], so
 * one bad manual tap cannot hide a backup that succeeded this morning, and a staleness read that
 * throws cannot hide a five-cycle failure streak.
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
     * A snapshot exists, nothing is failing, and reading how old it is threw.
     *
     * Distinct from [Failed] because the two are different sentences. [Failed] says a backup *cycle*
     * did not produce a snapshot — a fact the health surface persisted. This says the app cannot
     * currently describe the snapshot it has, which claims nothing about whether backup is working.
     * The failing counterpart of this state is `Failed(reason, LastSnapshot.AgeUnknown)`, not this:
     * a streak that is already resolved and in hand must not be dropped for a weaker sentence.
     */
    data object Unreadable : BackupRowUi

    /**
     * At least one cycle has failed since the last verified snapshot — **and what that snapshot is**.
     *
     * @property reason may be null; read the trap note above before keying anything on it.
     * @property lastSnapshot the snapshot the failure is happening *over*. It is here so a failure
     *   cannot erase a good backup from the row: a manual tap on bad wifi an hour after a successful
     *   automatic cycle is an ordinary sequence, and the daily cap then keeps the streak from
     *   clearing until midnight. It is also what [severity] reads to decide how loud to be.
     */
    data class Failed(val reason: BackupFailureReason?, val lastSnapshot: LastSnapshot) : BackupRowUi

    /** A verified snapshot exists, but it is older than the threshold **and** the ledger has moved since. */
    data class Stale(val daysSinceLastBackup: Int) : BackupRowUi

    /** A verified snapshot exists and nothing above applies. */
    data class UpToDate(val daysSinceLastBackup: Int) : BackupRowUi
}

/**
 * How loudly to draw the row — resolved here rather than in each platform's composable, for the
 * reason [BackupRowUi] exists at all.
 *
 * ### The rule, after review
 *
 * **[BackupRowSeverity.Danger] means the ledger is not protected and will not become protected on
 * its own.** It is deliberately not "something failed": a failed cycle over this morning's snapshot
 * is an update that did not happen, and the data is safe.
 *
 * That reads two ways, and the second one was missing. The first is [LastSnapshot.None] — nothing is
 * backed up at all. The second is a snapshot the staleness rule already calls stale *while cycles
 * are failing*: "backed up 30 days ago, failing ever since, with unsaved changes" is a ledger at
 * risk just as truly, and it used to render in the same amber as a one-off hiccup because severity
 * only ever escalated through `None`. A plain [BackupRowUi.Stale] stays [BackupRowSeverity.Warning]
 * precisely because it *will* self-heal — the next trigger fixes it. A failing one will not.
 *
 * [LastSnapshot.AgeUnknown] stays at [BackupRowSeverity.Warning] on purpose: not knowing the age is
 * not evidence the snapshot is old, and guessing loudly would cry wolf on a device that is fine.
 * [BackupRowUi.Never] likewise — a signed-in device that has not been backed up *yet* is a "not
 * yet", and the orchestrator's next trigger is what answers it.
 */
fun BackupRowUi.severity(): BackupRowSeverity = when (this) {
    BackupRowUi.NeedsAccount,
    BackupRowUi.BackingUp,
    BackupRowUi.Never,
    -> BackupRowSeverity.Normal

    BackupRowUi.Unreadable -> BackupRowSeverity.Warning

    is BackupRowUi.UpToDate -> BackupRowSeverity.Normal

    is BackupRowUi.Stale -> BackupRowSeverity.Warning

    is BackupRowUi.Failed -> when (val snapshot = lastSnapshot) {
        LastSnapshot.None -> BackupRowSeverity.Danger
        LastSnapshot.AgeUnknown -> BackupRowSeverity.Warning
        is LastSnapshot.DaysAgo -> if (snapshot.isStale) BackupRowSeverity.Danger else BackupRowSeverity.Warning
    }
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
