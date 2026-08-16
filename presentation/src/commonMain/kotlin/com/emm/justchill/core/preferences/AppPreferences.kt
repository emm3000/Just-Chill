package com.emm.justchill.core.preferences

import com.russhwolf.settings.Settings

/**
 * Onboarding state and the sync cursors, over [Settings] (multiplatform-settings).
 *
 * Backed by SharedPreferences on Android (via SharedPreferencesSettings, wired in CoreModule) and
 * NSUserDefaults on iOS (via NSUserDefaultsSettings, wired in KoinIos). Keys, value types, and the
 * -1L "never synced" sentinel are preserved verbatim from the former Android-only implementation so
 * existing installs keep their onboarding state and sync cursor.
 *
 * The backup keys are NOT here: `DefaultBackupMetadataStore` (`core/backup/`) owns them, because
 * everything left in this class is deleted whole with the sync engine in ADR 009 Phase 5 and the
 * backup metadata must survive that deletion.
 */
class AppPreferences(private val settings: Settings) {
    var firstLaunchSeen: Boolean
        get() = settings.getBoolean(KEY_FIRST_LAUNCH_SEEN, false)
        set(value) = settings.putBoolean(KEY_FIRST_LAUNCH_SEEN, value)

    /**
     * Returns the last-pulled-at ISO-8601 UTC cursor for [userId], or null if the user has
     * never successfully pulled (triggers a full re-pull, which is safe and idempotent).
     */
    fun lastPulledAt(userId: String): String? = settings.getStringOrNull(userKey(KEY_LAST_PULLED_AT_PREFIX, userId))

    /**
     * Persists [cursor] as the new watermark for [userId].
     */
    fun setLastPulledAt(userId: String, cursor: String) {
        settings.putString(userKey(KEY_LAST_PULLED_AT_PREFIX, userId), cursor)
    }

    /**
     * Returns the epoch-millis timestamp of the last successful sync for [userId], or null if
     * the user has never completed a sync on this device (sentinel -1L = never).
     */
    fun lastSyncedAt(userId: String): Long? {
        val value = settings.getLong(userKey(KEY_LAST_SYNCED_AT_PREFIX, userId), -1L)
        return if (value == -1L) null else value
    }

    /**
     * Persists [epochMillis] as the last-synced-at timestamp for [userId].
     */
    fun setLastSyncedAt(userId: String, epochMillis: Long) {
        settings.putLong(userKey(KEY_LAST_SYNCED_AT_PREFIX, userId), epochMillis)
    }

    /**
     * Removes both per-user sync metadata keys for [userId]:
     * the pull cursor ([KEY_LAST_PULLED_AT_PREFIX]) and the last-synced-at timestamp
     * ([KEY_LAST_SYNCED_AT_PREFIX]). Called when the user deletes their account so stale
     * cursor data does not interfere if the same device registers again.
     */
    fun clearSyncMetadata(userId: String) {
        settings.remove(userKey(KEY_LAST_PULLED_AT_PREFIX, userId))
        settings.remove(userKey(KEY_LAST_SYNCED_AT_PREFIX, userId))
    }

    /**
     * The one spelling of the per-user key layout: a fixed prefix followed by the user id.
     */
    private fun userKey(prefix: String, userId: String) = "$prefix$userId"

    private companion object {
        const val KEY_FIRST_LAUNCH_SEEN = "first_launch_seen"
        const val KEY_LAST_PULLED_AT_PREFIX = "last_pulled_at_"
        const val KEY_LAST_SYNCED_AT_PREFIX = "last_synced_at_"
    }
}
