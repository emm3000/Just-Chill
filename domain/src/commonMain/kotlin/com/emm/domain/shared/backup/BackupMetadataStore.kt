package com.emm.domain.shared.backup

/**
 * Port interface for persisting the per-user last-successful-backup-upload timestamp.
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
     * Removes the persisted backup watermark for [userId]. Called when the user deletes their
     * account so a stale timestamp does not survive to a later registration: without this clear,
     * the same device registering again with a new account would still read a timestamp claiming a
     * backup already exists, the new account's bucket would be empty, and the first snapshot for it
     * would never fire.
     */
    fun clear(userId: String)
}
