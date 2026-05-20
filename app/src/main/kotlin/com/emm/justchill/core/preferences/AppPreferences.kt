package com.emm.justchill.core.preferences

import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreferences(private val prefs: SharedPreferences) {
    var firstLaunchSeen: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH_SEEN, false)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_LAUNCH_SEEN, value) }

    private companion object {
        const val KEY_FIRST_LAUNCH_SEEN = "first_launch_seen"
    }
}
