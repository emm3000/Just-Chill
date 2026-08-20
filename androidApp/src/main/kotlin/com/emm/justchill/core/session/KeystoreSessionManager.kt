package com.emm.justchill.core.session

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

internal const val ENCRYPTED_SESSION_KEY = "session_encrypted"

// The key SettingsSessionManager wrote, and the only reason this class reads two of them.
internal const val LEGACY_SESSION_KEY = "session"

/**
 * `encodeDefaults` is what SettingsSessionManager wrote with; a payload migrated off it decodes to a
 * different session without it.
 */
internal val sessionJson: Json = Json { encodeDefaults = true }

internal class KeystoreSessionManager(
    private val prefs: SharedPreferences,
    private val cipher: SessionCipher,
    private val json: Json,
) : SessionManager {

    override suspend fun saveSession(session: UserSession) {
        prefs.edit { putString(ENCRYPTED_SESSION_KEY, cipher.encrypt(json.encodeToString(session))) }
    }

    override suspend fun loadSession(): UserSession {
        val stored = prefs.getString(ENCRYPTED_SESSION_KEY, null)
        val plaintext = if (stored == null) adoptLegacySession() else cipher.decrypt(stored)
        return json.decodeFromString(plaintext ?: throw NoSessionFoundException())
    }

    override suspend fun deleteSession() {
        prefs.edit {
            remove(ENCRYPTED_SESSION_KEY)
            remove(LEGACY_SESSION_KEY)
        }
    }

    // Runs at most once per install: it removes the cleartext in the same edit that writes the
    // ciphertext, so the branch that reaches it is unreachable on every later load.
    private fun adoptLegacySession(): String? {
        val legacy = prefs.getString(LEGACY_SESSION_KEY, null) ?: return null
        prefs.edit {
            putString(ENCRYPTED_SESSION_KEY, cipher.encrypt(legacy))
            remove(LEGACY_SESSION_KEY)
        }
        return legacy
    }
}
