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

// Without encodeDefaults a written payload omits expiresAt, and UserSession's default recomputes it
// as now + expiresIn on the next read — silently extending the session.
internal val sessionJson: Json = Json { encodeDefaults = true }

internal class KeystoreSessionManager(
    private val prefs: SharedPreferences,
    private val cipher: SessionCipher,
    private val diagnostics: DiagnosticsLogger,
) : SessionManager {

    // Every read-then-write of the two keys runs under this monitor: the startup sweep and the
    // Supabase client's first load race by design, and unsynchronised they can interleave into a
    // downgrade — the sweep writing the older session back over the client's newer one.
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

    // Re-encrypts the cleartext session a pre-Keystore build left behind and drops the cleartext key.
    // Runs at launch because loadSession, the only other caller, hangs off a lazy Koin single that
    // nothing on the startup path resolves; never throws, so a dead Keystore does not kill onCreate.
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

// Only these prove the payload can never read back — a bad tag, an invalidated key, or garbage the
// encoder could not have produced. Everything else is transient: discarding on it would cost a
// re-login for a session that might read fine next launch, so the default is to keep it.
internal fun Throwable.willNeverReadBack(): Boolean = when (this) {
    is AEADBadTagException,
    is KeyPermanentlyInvalidatedException,
    is IllegalArgumentException,
    -> true

    else -> false
}
