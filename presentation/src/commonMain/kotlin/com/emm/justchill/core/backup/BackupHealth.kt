package com.emm.justchill.core.backup

import com.emm.domain.shared.backup.BackupFailureReason

/**
 * Whether snapshot backup is working, for the account signed in right now.
 *
 * The state surface [BackupEvent]'s KDoc reserves for ADR 009 Phase 3 and refuses to hold itself: an
 * event is a one-shot answer to a tap, and this is a standing fact that a screen opened an hour
 * later must still be able to read. Published on [BackupController.health].
 *
 * ### It is per-account, and it follows the session
 *
 * Everything here is stored per user id, so the value published is whichever account the session
 * gate currently holds — [None] while there is none. A backup that fails for an account which signs
 * out mid-cycle is still recorded against that account, but it is not published: whoever is signed
 * in now did not fail anything, and showing them another user's streak is the same class of lie as
 * recording another user's watermark.
 *
 * ### [None] is "nothing known", not "healthy"
 *
 * It is also what the orchestrator publishes when `SNAPSHOT_BACKUP_ENABLED` is false, because
 * `start()` is never called and the session gate that seeds this never runs. A UI must read
 * `lastSuccessfulBackupAt == null` as "no backup on this device", never as "up to date" — the flag
 * gate is why that distinction is not academic today.
 *
 * @property lastSuccessfulBackupAt epoch millis of the last verified, recorded snapshot; null if
 *   this device has never completed one for this account.
 * @property consecutiveFailures cycles failed in a row since the last verified success. Zero after
 *   any success, so a non-zero value means backup is broken *now*.
 * @property lastFailureReason why the most recent failure failed; null when nothing has failed
 *   since the last success. Non-null exactly when [consecutiveFailures] is non-zero.
 */
data class BackupHealth(
    val lastSuccessfulBackupAt: Long?,
    val consecutiveFailures: Int,
    val lastFailureReason: BackupFailureReason?,
) {
    companion object {
        /** No account, no history: what the orchestrator publishes while signed out or unstarted. */
        val None = BackupHealth(lastSuccessfulBackupAt = null, consecutiveFailures = 0, lastFailureReason = null)
    }
}
