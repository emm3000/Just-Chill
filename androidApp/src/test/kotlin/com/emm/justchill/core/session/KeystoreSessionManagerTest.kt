package com.emm.justchill.core.session

import android.content.SharedPreferences
import com.emm.domain.shared.logging.DiagnosticsLogger
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.security.KeyStoreException
import javax.crypto.AEADBadTagException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KeystoreSessionManagerTest {

    private val prefs = FakePreferences()
    private val cipher = FakeSessionCipher()
    private val diagnostics = RecordingDiagnosticsLogger()
    private val manager = KeystoreSessionManager(prefs, cipher, diagnostics)

    private val session = UserSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresIn = 3600L,
        tokenType = "bearer",
    )

    @Test
    fun `a saved session comes back, and what lands in the file is not the session`() = runTest {
        manager.saveSession(session)

        assertEquals(session, manager.loadSession())
        assertNotEquals(sessionJson.encodeToString(session), prefs.values[ENCRYPTED_SESSION_KEY])
    }

    @Test
    fun `a legacy plaintext session survives the move and stops existing in the clear`() = runTest {
        prefs.values[LEGACY_SESSION_KEY] = sessionJson.encodeToString(session)

        assertEquals(session, manager.loadSession())
        assertFalse(prefs.values.containsKey(LEGACY_SESSION_KEY))
        assertTrue(prefs.values.containsKey(ENCRYPTED_SESSION_KEY))
    }

    @Test
    fun `the move happens once, not on every load`() = runTest {
        prefs.values[LEGACY_SESSION_KEY] = sessionJson.encodeToString(session)

        manager.loadSession()
        assertEquals(session, manager.loadSession())
        assertEquals(1, cipher.encryptions)
    }

    @Test
    fun `a session the cipher can no longer read reads as no session at all`() = runTest {
        manager.saveSession(session)
        cipher.failure = AEADBadTagException("mac check in GCM failed")

        assertFailsWith<NoSessionFoundException> { manager.loadSession() }
    }

    @Test
    fun `a plaintext key a downgraded build wrote back wins, and stops existing`() = runTest {
        val newer = session.copy(refreshToken = "written-by-the-older-build")
        manager.saveSession(session)
        prefs.values[LEGACY_SESSION_KEY] = sessionJson.encodeToString(newer)

        assertEquals(newer, manager.loadSession())
        assertFalse(prefs.values.containsKey(LEGACY_SESSION_KEY))
    }

    @Test
    fun `an empty store reports absence the way supabase expects`() = runTest {
        assertFailsWith<NoSessionFoundException> { manager.loadSession() }
    }

    @Test
    fun `signing out leaves neither the encrypted nor the legacy key behind`() = runTest {
        manager.saveSession(session)
        prefs.values[LEGACY_SESSION_KEY] = sessionJson.encodeToString(session)

        manager.deleteSession()

        assertFalse(prefs.values.containsKey(ENCRYPTED_SESSION_KEY))
        assertFalse(prefs.values.containsKey(LEGACY_SESSION_KEY))
    }

    @Test
    fun `the launch sweep takes the plaintext key away and leaves a readable session behind`() = runTest {
        prefs.values[LEGACY_SESSION_KEY] = sessionJson.encodeToString(session)

        manager.sweepLegacySession()

        assertFalse(prefs.values.containsKey(LEGACY_SESSION_KEY))
        assertEquals(session, manager.loadSession())
    }

    @Test
    fun `the launch sweep writes nothing and reports nothing when there is no plaintext key`() = runTest {
        manager.sweepLegacySession()

        assertTrue(prefs.values.isEmpty())
        assertEquals(0, cipher.encryptions)
        assertTrue(diagnostics.reports.isEmpty())
    }

    @Test
    fun `a keystore that cannot encrypt is reported by the launch sweep, never thrown at onCreate`() = runTest {
        prefs.values[LEGACY_SESSION_KEY] = sessionJson.encodeToString(session)
        cipher.writable = false

        manager.sweepLegacySession()

        assertEquals(1, diagnostics.reports.size)
        assertTrue(prefs.values.containsKey(LEGACY_SESSION_KEY))
    }

    @Test
    fun `a store that works end to end reports nothing at all`() = runTest {
        manager.saveSession(session)
        manager.loadSession()
        manager.sweepLegacySession()
        manager.deleteSession()

        assertTrue(diagnostics.reports.isEmpty())
    }

    @Test
    fun `a session whose key is gone is reported once, not on every load after it`() = runTest {
        manager.saveSession(session)
        cipher.failure = AEADBadTagException("mac check in GCM failed")

        repeat(2) { assertFailsWith<NoSessionFoundException> { manager.loadSession() } }

        assertEquals(1, diagnostics.reports.size)
    }

    @Test
    fun `a session whose key is gone stops occupying the file`() = runTest {
        manager.saveSession(session)
        cipher.failure = AEADBadTagException("mac check in GCM failed")

        assertFailsWith<NoSessionFoundException> { manager.loadSession() }

        assertFalse(prefs.values.containsKey(ENCRYPTED_SESSION_KEY))
    }

    @Test
    fun `a session the provider merely failed to read once is kept for the next launch`() = runTest {
        manager.saveSession(session)
        val stored = prefs.values[ENCRYPTED_SESSION_KEY]
        cipher.failure = KeyStoreException("provider unavailable")

        assertFailsWith<NoSessionFoundException> { manager.loadSession() }

        assertEquals(stored, prefs.values[ENCRYPTED_SESSION_KEY])
    }

    @Test
    fun `a session kept for the next launch reads back once the provider recovers`() = runTest {
        manager.saveSession(session)
        cipher.failure = KeyStoreException("provider unavailable")
        assertFailsWith<NoSessionFoundException> { manager.loadSession() }

        cipher.failure = null

        assertEquals(session, manager.loadSession())
    }

    @Test
    fun `the report carries the cause, which is the half that reaches the non-fatal channel`() = runTest {
        manager.saveSession(session)
        val failure = AEADBadTagException("mac check in GCM failed")
        cipher.failure = failure

        assertFailsWith<NoSessionFoundException> { manager.loadSession() }

        assertSame(failure, diagnostics.reports.single().cause)
    }

    @Test
    fun `neither the report nor the cause it carries names any part of what was stored`() = runTest {
        manager.saveSession(session)
        val stored = prefs.values.getValue(ENCRYPTED_SESSION_KEY).orEmpty()
        cipher.failure = AEADBadTagException("mac check in GCM failed")

        assertFailsWith<NoSessionFoundException> { manager.loadSession() }

        val report = diagnostics.reports.single()
        val uploaded = report.message + report.cause?.stackTraceToString()
        assertFalse(uploaded.contains(session.refreshToken))
        assertFalse(uploaded.contains(session.accessToken))
        assertFalse(uploaded.contains(stored))
    }

    @Test
    fun `an empty store is not worth reporting`() = runTest {
        assertFailsWith<NoSessionFoundException> { manager.loadSession() }

        assertTrue(diagnostics.reports.isEmpty())
    }
}

private class FakeSessionCipher : SessionCipher {

    var failure: Throwable? = null
    var writable: Boolean = true
    var encryptions: Int = 0

    override fun encrypt(plaintext: String): String {
        check(writable) { "keystore unavailable" }
        encryptions++
        return plaintext.reversed()
    }

    override fun decrypt(payload: String): Result<String> =
        failure?.let { Result.failure(it) } ?: Result.success(payload.reversed())
}

private class Report(val message: String, val cause: Throwable?)

private class RecordingDiagnosticsLogger : DiagnosticsLogger {

    val reports: MutableList<Report> = mutableListOf()

    override fun warn(message: String, throwable: Throwable?) {
        reports += Report(message, throwable)
    }
}

private class FakePreferences : SharedPreferences {

    val values: MutableMap<String, String?> = mutableMapOf()

    override fun getString(key: String, defValue: String?): String? = values[key] ?: defValue

    override fun edit(): SharedPreferences.Editor = FakeEditor(values)

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun getAll(): MutableMap<String, *> = values

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues

    override fun getInt(key: String, defValue: Int): Int = defValue

    override fun getLong(key: String, defValue: Long): Long = defValue

    override fun getFloat(key: String, defValue: Float): Float = defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean = defValue

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit
}

private class FakeEditor(private val values: MutableMap<String, String?>) : SharedPreferences.Editor {

    override fun putString(key: String, value: String?): SharedPreferences.Editor = also { values[key] = value }

    override fun remove(key: String): SharedPreferences.Editor = also { values.remove(key) }

    override fun clear(): SharedPreferences.Editor = also { values.clear() }

    override fun commit(): Boolean = true

    override fun apply() = Unit

    override fun putStringSet(key: String, value: MutableSet<String>?): SharedPreferences.Editor = this

    override fun putInt(key: String, value: Int): SharedPreferences.Editor = this

    override fun putLong(key: String, value: Long): SharedPreferences.Editor = this

    override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this

    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = this
}
