package com.emm.justchill.core.preferences

import com.russhwolf.settings.Settings

// Key strings and the -1L "never" sentinel are load-bearing: existing installs already hold values
// under them. Backup keys are not here — DefaultBackupMetadataStore (core/backup/) owns those,
// because everything in this class goes with the sync engine in ADR 009 Phase 5.
class AppPreferences(private val settings: Settings) {
    var firstLaunchSeen: Boolean
        get() = settings.getBoolean(KEY_FIRST_LAUNCH_SEEN, false)
        set(value) = settings.putBoolean(KEY_FIRST_LAUNCH_SEEN, value)

    // null triggers a full re-pull, which is safe and idempotent.
    fun lastPulledAt(userId: String): String? = settings.getStringOrNull(userKey(KEY_LAST_PULLED_AT_PREFIX, userId))

    fun setLastPulledAt(userId: String, cursor: String) {
        settings.putString(userKey(KEY_LAST_PULLED_AT_PREFIX, userId), cursor)
    }

    fun lastSyncedAt(userId: String): Long? {
        val value = settings.getLong(userKey(KEY_LAST_SYNCED_AT_PREFIX, userId), NEVER)
        return if (value == NEVER) null else value
    }

    fun setLastSyncedAt(userId: String, epochMillis: Long) {
        settings.putLong(userKey(KEY_LAST_SYNCED_AT_PREFIX, userId), epochMillis)
    }

    fun clearSyncMetadata(userId: String) {
        settings.remove(userKey(KEY_LAST_PULLED_AT_PREFIX, userId))
        settings.remove(userKey(KEY_LAST_SYNCED_AT_PREFIX, userId))
    }

    private fun userKey(prefix: String, userId: String) = "$prefix$userId"

    private companion object {
        const val NEVER = -1L
        const val KEY_FIRST_LAUNCH_SEEN = "first_launch_seen"
        const val KEY_LAST_PULLED_AT_PREFIX = "last_pulled_at_"
        const val KEY_LAST_SYNCED_AT_PREFIX = "last_synced_at_"
    }
}
