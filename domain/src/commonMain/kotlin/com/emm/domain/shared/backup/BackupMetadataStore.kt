package com.emm.domain.shared.backup

/**
 * Port interface for persisting per-user backup health: the last-successful-upload timestamp, and
 * the failure streak beside it.
 *
 * ADR 009 Phase 2c-i introduced the watermark itself (`AppPreferences.lastSuccessfulBackupAt`) with
 * no seam of its own: the write and the account-deletion clear both rode `SyncCursorStore.clear`
 * because that was the only account-deletion seam that existed at the time, and inventing a
 * dedicated port for a key nothing read yet would have been YAGNI. 2c-iii-a is where the writer of
 * that timestamp arrives, so it is where this seam belongs.
 *
 * Implemented in `:presentation` over `AppPreferences`, the same way `SyncCursorStore` is
 * implemented by `DefaultSyncCursorStore` — see that pair for the pattern. Unlike `SyncCursorStore`,
 * this port does NOT live under a package ADR 009 Phase 5 deletes: the backup watermark is the
 * pipeline this whole plan is building toward, not the engine it removes, so both the interface and
 * its implementation stay in neutral packages that survive Phase 5 unchanged.
 */
interface BackupMetadataStore {

    /**
     * Returns the epoch-millis timestamp of the last successful, verified backup upload for
     * [userId], or null if this device has never completed one.
     */
    fun lastSuccessfulBackupAt(userId: String): Long?

    /**
     * Persists [epochMillis] as the last-successful-backup-at timestamp for [userId].
     */
    fun setLastSuccessfulBackupAt(userId: String, epochMillis: Long)

    /**
     * The failure streak for [userId], or [BackupFailureState.None] when nothing is recorded.
     *
     * Persisted rather than in-memory (ADR 009 Phase 3): a failure indicator that a restart resets
     * would tell a device whose backups have been failing for a week that everything is fine, which
     * is the shape hard constraint 4 forbids.
     */
    fun failureState(userId: String): BackupFailureState

    /**
     * Increments [userId]'s failure streak and records [reason] as the newest one, returning the
     * state that was just written.
     *
     * The increment lives behind the port rather than in the caller because the count and the reason
     * are stored together and must move together — see [BackupFailureState]. Returning the new state
     * saves the caller a second read whose answer this call already knows.
     */
    fun recordFailure(userId: String, reason: BackupFailureReason): BackupFailureState

    /**
     * Resets [userId]'s failure streak to [BackupFailureState.None].
     *
     * Deliberately separate from [setLastSuccessfulBackupAt] rather than folded into it: the
     * watermark is what the once-a-day cap reads and the streak is what the health indicator reads,
     * and a cycle that recorded a watermark for an account it can no longer see must write neither.
     * Both calls sit inside the same account re-check for that reason.
     */
    fun clearFailures(userId: String)

    /**
     * Removes the persisted backup watermark **and the failure streak** for [userId]. Called when
     * the user deletes their account so neither survives to a later registration: without this
     * clear, the same device registering again with a new account would still read a timestamp
     * claiming a backup already exists — the new account's bucket would be empty and the first
     * snapshot for it would never fire — and would still read the deleted account's failure count,
     * showing a brand-new account a streak of failures it never had.
     */
    fun clear(userId: String)
}
