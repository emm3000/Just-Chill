package com.emm.justchill.core.session

import android.content.SharedPreferences
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.core.content.edit
import com.emm.domain.shared.logging.DiagnosticsLogger
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json
import javax.crypto.AEADBadTagException

internal const val ENCRYPTED_SESSION_KEY = "session_encrypted"

// The key SettingsSessionManager wrote, and the only reason this class reads two of them.
internal const val LEGACY_SESSION_KEY = "session"

/**
 * Without `encodeDefaults` a written payload omits `expiresAt`, and `UserSession`'s default
 * recomputes it as `now + expiresIn` on the next read — silently extending the session.
 */
internal val sessionJson: Json = Json { encodeDefaults = true }

internal class KeystoreSessionManager(
    private val prefs: SharedPreferences,
    private val cipher: SessionCipher,
    private val diagnostics: DiagnosticsLogger,
) : SessionManager {

    // Every read-then-write of the two keys runs under this monitor. The startup sweep and the
    // Supabase client's own first load race by design, and unsynchronised they interleave into a
    // downgrade: the sweep reads the legacy value, the client loads and refreshes, and the sweep
    // then writes the older session back over the newer one.
    private val storeLock = Any()

    override suspend fun saveSession(session: UserSession) {
        synchronized(storeLock) { writeEncrypted(sessionJson.encodeToString(session)) }
    }

    override suspend fun loadSession(): UserSession {
        val plaintext = synchronized(storeLock) { adoptLegacySession() ?: readEncryptedSession() }
        return sessionJson.decodeFromString(plaintext ?: throw NoSessionFoundException())
    }

    override suspend fun deleteSession() {
        synchronized(storeLock) {
            prefs.edit {
                remove(ENCRYPTED_SESSION_KEY)
                remove(LEGACY_SESSION_KEY)
            }
        }
    }

    /**
     * Re-encrypts the cleartext session a pre-Keystore build left behind and drops the cleartext key.
     *
     * Called at launch because [loadSession] — the only other caller of the same move — hangs off a
     * lazy Koin `single` that nothing on the startup path resolves: on an existing install the
     * cleartext refresh token otherwise survives every launch until the user opens the account
     * screen. Never throws, so a Keystore that cannot serve a key does not kill `onCreate`.
     */
    fun sweepLegacySession() {
        runCatching { synchronized(storeLock) { adoptLegacySession() } }
            .onFailure { diagnostics.warn("Legacy session sweep failed; the cleartext key survives", it) }
    }

    private fun readEncryptedSession(): String? {
        val stored = prefs.getString(ENCRYPTED_SESSION_KEY, null) ?: return null
        val read = cipher.decrypt(stored)
        read.exceptionOrNull()?.let(::reportUnreadableSession)
        return read.getOrNull()
    }

    private fun reportUnreadableSession(cause: Throwable) {
        if (!cause.willNeverReadBack()) {
            diagnostics.warn("Stored session could not be decrypted; kept for the next launch", cause)
            return
        }
        // Dropped only here, because KeystoreSessionCipher deletes an unusable alias and generates
        // a new one: the key that could read this blob is already gone, so keeping it would repeat
        // this warning on every cold start for the life of the install.
        prefs.edit { remove(ENCRYPTED_SESSION_KEY) }
        diagnostics.warn("Stored session could not be decrypted; discarded and signed out", cause)
    }

    // Sideloading the previous APK re-creates the cleartext key and makes it the fresher of the
    // two, so this runs ahead of the encrypted key on every load rather than only on first migration.
    private fun adoptLegacySession(): String? {
        val legacy = prefs.getString(LEGACY_SESSION_KEY, null) ?: return null
        writeEncrypted(legacy)
        return legacy
    }

    private fun writeEncrypted(plaintext: String) {
        prefs.edit {
            putString(ENCRYPTED_SESSION_KEY, cipher.encrypt(plaintext))
            remove(LEGACY_SESSION_KEY)
        }
    }
}

// Only these prove the payload can never read back: a GCM tag mismatch means it was written under a
// different key, an invalidated key is not coming back, and a payload the encoder cannot have
// produced is garbage. Everything else — a provider that failed to load this once — is transient,
// and discarding on it costs a re-login for a session that would have read fine next launch, so the
// default is to keep. A KeyStoreException is deliberately absent: it reports an unavailable provider
// or an uninitialised store, neither of which says anything about this alias.
private fun Throwable.willNeverReadBack(): Boolean = when (this) {
    is AEADBadTagException,
    is KeyPermanentlyInvalidatedException,
    is IllegalArgumentException,
    is IndexOutOfBoundsException,
    -> true

    else -> false
}
