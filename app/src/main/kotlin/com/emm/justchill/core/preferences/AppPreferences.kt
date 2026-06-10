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

    private fun lastPulledAtKey(userId: String) = "${KEY_LAST_PULLED_AT_PREFIX}$userId"

    private companion object {
        const val KEY_FIRST_LAUNCH_SEEN = "first_launch_seen"
        const val KEY_LAST_PULLED_AT_PREFIX = "last_pulled_at_"
    }
}
