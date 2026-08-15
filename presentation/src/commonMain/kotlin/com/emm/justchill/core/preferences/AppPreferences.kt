package com.emm.justchill.core.preferences

import com.russhwolf.settings.Settings

/**
 * Single commonMain key-value preferences facade over [Settings] (multiplatform-settings).
 *
 * Backed by SharedPreferences on Android (via SharedPreferencesSettings, wired in CoreModule) and
 * NSUserDefaults on iOS (via NSUserDefaultsSettings, wired in KoinIos). Keys, value types, and the
 * -1L "never synced" sentinel are preserved verbatim from the former Android-only implementation so
 * existing installs keep their onboarding state and sync cursor.
 */
class AppPreferences(private val settings: Settings) {
    var firstLaunchSeen: Boolean
        get() = settings.getBoolean(KEY_FIRST_LAUNCH_SEEN, false)
        set(value) = settings.putBoolean(KEY_FIRST_LAUNCH_SEEN, value)

    /**
     * Returns the last-pulled-at ISO-8601 UTC cursor for [userId], or null if the user has
     * never successfully pulled (triggers a full re-pull, which is safe and idempotent).
     */
    fun lastPulledAt(userId: String): String? = settings.getStringOrNull(lastPulledAtKey(userId))

    /**
     * Persists [cursor] as the new watermark for [userId].
     */
    fun setLastPulledAt(userId: String, cursor: String) {
        settings.putString(lastPulledAtKey(userId), cursor)
    }

    /**
     * Returns the epoch-millis timestamp of the last successful sync for [userId], or null if
     * the user has never completed a sync on this device (sentinel -1L = never).
     */
    fun lastSyncedAt(userId: String): Long? {
        val value = settings.getLong(lastSyncedAtKey(userId), -1L)
        return if (value == -1L) null else value
    }

    /**
     * Persists [epochMillis] as the last-synced-at timestamp for [userId].
     */
    fun setLastSyncedAt(userId: String, epochMillis: Long) {
        settings.putLong(lastSyncedAtKey(userId), epochMillis)
    }

    /**
     * Removes both per-user sync metadata keys for [userId]:
     * the pull cursor ([KEY_LAST_PULLED_AT_PREFIX]) and the last-synced-at timestamp
     * ([KEY_LAST_SYNCED_AT_PREFIX]). Called when the user deletes their account so stale
     * cursor data does not interfere if the same device registers again.
     */
    fun clearSyncMetadata(userId: String) {
        settings.remove(lastPulledAtKey(userId))
        settings.remove(lastSyncedAtKey(userId))
    }

    /**
     * Returns the epoch-millis timestamp of the last successful, verified backup upload for
     * [userId], or null if this device has never completed one (sentinel -1L = never).
     */
    fun lastSuccessfulBackupAt(userId: String): Long? {
        val value = settings.getLong(lastSuccessfulBackupAtKey(userId), -1L)
        return if (value == -1L) null else value
    }

    /**
     * Persists [epochMillis] as the last-successful-backup-at timestamp for [userId].
     */
    fun setLastSuccessfulBackupAt(userId: String, epochMillis: Long) {
        settings.putLong(lastSuccessfulBackupAtKey(userId), epochMillis)
    }

    /**
     * Removes the last-successful-backup-at key ([KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX]) for
     * [userId].
     *
     * Deliberately a SEPARATE function from [clearSyncMetadata], not a third line added to it:
     * `clearSyncMetadata` and everything sync-named is deleted whole in ADR 009 Phase 5, and this
     * key must survive that deletion. Called through `BackupMetadataStore`'s implementation,
     * `DefaultBackupMetadataStore` (`:presentation`, `core/backup/`) — its own seam, not a side
     * effect of `DefaultSyncCursorStore.clear`. Failure this prevents: a user deletes their account
     * and registers again on the same device — with no clear, the stale timestamp claims a backup
     * already happened, the new account's bucket is empty, and the first snapshot for it never
     * fires.
     */
    fun clearBackupMetadata(userId: String) {
        settings.remove(lastSuccessfulBackupAtKey(userId))
    }

    private fun lastPulledAtKey(userId: String) = "${KEY_LAST_PULLED_AT_PREFIX}$userId"
    private fun lastSyncedAtKey(userId: String) = "${KEY_LAST_SYNCED_AT_PREFIX}$userId"
    private fun lastSuccessfulBackupAtKey(userId: String) = "${KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX}$userId"

    private companion object {
        const val KEY_FIRST_LAUNCH_SEEN = "first_launch_seen"
        const val KEY_LAST_PULLED_AT_PREFIX = "last_pulled_at_"
        const val KEY_LAST_SYNCED_AT_PREFIX = "last_synced_at_"
        const val KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX = "last_successful_backup_at_"
    }
}
