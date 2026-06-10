package com.emm.justchill.core.preferences

import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreferences(private val prefs: SharedPreferences) {
    var firstLaunchSeen: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH_SEEN, false)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_LAUNCH_SEEN, value) }

    /**
     * Returns the last-pulled-at ISO-8601 UTC cursor for [userId], or null if the user has
     * never successfully pulled (triggers a full re-pull, which is safe and idempotent).
     */
    fun lastPulledAt(userId: String): String? = prefs.getString(lastPulledAtKey(userId), null)

    /**
     * Persists [cursor] as the new watermark for [userId].
     */
    fun setLastPulledAt(userId: String, cursor: String) {
        prefs.edit { putString(lastPulledAtKey(userId), cursor) }
    }

    /**
     * Returns the epoch-millis timestamp of the last successful sync for [userId], or null if
     * the user has never completed a sync on this device.
     */
    fun lastSyncedAt(userId: String): Long? {
        val value = prefs.getLong(lastSyncedAtKey(userId), -1L)
        return if (value == -1L) null else value
    }

    /**
     * Persists [epochMillis] as the last-synced-at timestamp for [userId].
     */
    fun setLastSyncedAt(userId: String, epochMillis: Long) {
        prefs.edit { putLong(lastSyncedAtKey(userId), epochMillis) }
    }

    /**
     * Removes both per-user sync metadata keys for [userId]:
     * the pull cursor ([KEY_LAST_PULLED_AT_PREFIX]) and the last-synced-at timestamp
     * ([KEY_LAST_SYNCED_AT_PREFIX]). Called when the user deletes their account so stale
     * cursor data does not interfere if the same device registers again.
     */
    fun clearSyncMetadata(userId: String) {
        prefs.edit {
            remove(lastPulledAtKey(userId))
            remove(lastSyncedAtKey(userId))
        }
    }

    private fun lastPulledAtKey(userId: String) = "${KEY_LAST_PULLED_AT_PREFIX}$userId"
    private fun lastSyncedAtKey(userId: String) = "${KEY_LAST_SYNCED_AT_PREFIX}$userId"

    private companion object {
        const val KEY_FIRST_LAUNCH_SEEN = "first_launch_seen"
        const val KEY_LAST_PULLED_AT_PREFIX = "last_pulled_at_"
        const val KEY_LAST_SYNCED_AT_PREFIX = "last_synced_at_"
    }
}
