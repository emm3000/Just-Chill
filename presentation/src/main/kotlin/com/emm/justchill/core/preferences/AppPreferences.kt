package com.emm.justchill.core.preferences

import com.russhwolf.settings.Settings

// Key string is load-bearing: existing installs already hold a value under it. Backup keys are not
// here — DefaultBackupMetadataStore (core/backup/) owns those.
class AppPreferences(private val settings: Settings) {
    var firstLaunchSeen: Boolean
        get() = settings.getBoolean(KEY_FIRST_LAUNCH_SEEN, false)
        set(value) = settings.putBoolean(KEY_FIRST_LAUNCH_SEEN, value)

    private companion object {
        const val KEY_FIRST_LAUNCH_SEEN = "first_launch_seen"
    }
}
