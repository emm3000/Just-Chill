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
 * ### Read [consecutiveFailures] to decide whether to warn, never [lastFailureReason]
 *
 * The reason is a **label on** the streak, not the existence of one. It is null whenever nothing has
 * failed, and also whenever the persisted reason names something this build cannot resolve —
 * [BackupFailureReason.fromNameOrNull] answers null rather than throwing for a member that was
 * renamed or removed under a device that upgraded. That degradation is deliberate: a health
 * indicator must not crash the app it is reporting on. The consequence for a UI is that
 * `(5, null)` is a real, reachable value, so `if (lastFailureReason != null) showWarning()` hides a
 * device that has failed five times running. Warn on `consecutiveFailures > 0`; use the reason only
 * to choose the wording, with a fallback for null.
 *
 * @property lastSuccessfulBackupAt epoch millis of the last verified, recorded snapshot; null if
 *   this device has never completed one for this account.
 * @property consecutiveFailures cycles failed in a row since the last verified success. Zero after
 *   any success, so a non-zero value means backup is broken *now*. This is the warning condition.
 * @property lastFailureReason why the most recent failure failed. Null when nothing has failed since
 *   the last success, **and also** when the stored reason cannot be resolved by this build.
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
