package com.emm.justchill.core.session

import android.content.SharedPreferences
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KeystoreSessionManagerTest {

    private val prefs = FakePreferences()
    private val cipher = FakeSessionCipher()
    private val manager = KeystoreSessionManager(prefs, cipher)

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
        cipher.readable = false

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
}

private class FakeSessionCipher : SessionCipher {

    var readable: Boolean = true
    var encryptions: Int = 0

    override fun encrypt(plaintext: String): String {
        encryptions++
        return plaintext.reversed()
    }

    override fun decrypt(payload: String): String? = payload.reversed().takeIf { readable }
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
