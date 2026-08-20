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
 * Without `encodeDefaults` a written payload omits `expiresAt`, and `UserSession`'s default
 * recomputes it as `now + expiresIn` on the next read — silently extending the session.
 */
internal val sessionJson: Json = Json { encodeDefaults = true }

internal class KeystoreSessionManager(private val prefs: SharedPreferences, private val cipher: SessionCipher) :
    SessionManager {

    override suspend fun saveSession(session: UserSession) {
        prefs.edit {
            putString(ENCRYPTED_SESSION_KEY, cipher.encrypt(sessionJson.encodeToString(session)))
            remove(LEGACY_SESSION_KEY)
        }
    }

    override suspend fun loadSession(): UserSession {
        val plaintext = adoptLegacySession() ?: readEncryptedSession()
        return sessionJson.decodeFromString(plaintext ?: throw NoSessionFoundException())
    }

    override suspend fun deleteSession() {
        prefs.edit {
            remove(ENCRYPTED_SESSION_KEY)
            remove(LEGACY_SESSION_KEY)
        }
    }

    private fun readEncryptedSession(): String? = prefs.getString(ENCRYPTED_SESSION_KEY, null)?.let(cipher::decrypt)

    // Sideloading the previous APK re-creates the cleartext key and makes it the fresher of the
    // two, so this runs ahead of the encrypted key on every load rather than only on first migration.
    private fun adoptLegacySession(): String? {
        val legacy = prefs.getString(LEGACY_SESSION_KEY, null) ?: return null
        prefs.edit {
            putString(ENCRYPTED_SESSION_KEY, cipher.encrypt(legacy))
            remove(LEGACY_SESSION_KEY)
        }
        return legacy
    }
}
