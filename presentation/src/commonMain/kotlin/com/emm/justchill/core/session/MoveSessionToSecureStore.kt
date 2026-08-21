package com.emm.justchill.core.session

import com.russhwolf.settings.Settings
import io.github.jan.supabase.auth.SettingsSessionManager

private const val SESSION_KEY = SettingsSessionManager.SETTINGS_KEY

internal fun moveSessionToSecureStore(legacy: Settings, secure: Settings) {
    val session = legacy.getStringOrNull(SESSION_KEY) ?: return
    // Only a build predating this move can still write the legacy store, so a value found here is
    // always newer than whatever the secure store holds; it always overwrites.
    secure.putString(SESSION_KEY, session)
    legacy.remove(SESSION_KEY)
}
