package com.emm.justchill.core.session

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.github.jan.supabase.auth.SettingsSessionManager
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private const val SESSION_KEY = SettingsSessionManager.SETTINGS_KEY

class MoveSessionToSecureStoreTest {

    private lateinit var legacy: MapSettings
    private lateinit var secure: MapSettings

    @Before
    fun setUp() {
        legacy = MapSettings()
        secure = MapSettings()
    }

    @Test
    fun `a cleartext session lands in the secure store and leaves the legacy store empty`() {
        legacy.putString(SESSION_KEY, "{\"refresh_token\":\"legacy\"}")

        moveSessionToSecureStore(legacy, secure)

        assertEquals("{\"refresh_token\":\"legacy\"}", secure.getStringOrNull(SESSION_KEY))
        assertNull(legacy.getStringOrNull(SESSION_KEY))
    }

    @Test
    fun `a cleartext session overwrites one already in the secure store`() {
        legacy.putString(SESSION_KEY, "{\"refresh_token\":\"legacy\"}")
        secure.putString(SESSION_KEY, "{\"refresh_token\":\"secure\"}")

        moveSessionToSecureStore(legacy, secure)

        assertEquals("{\"refresh_token\":\"legacy\"}", secure.getStringOrNull(SESSION_KEY))
    }

    @Test
    fun `a cleartext session overwriting the secure store still empties the legacy store`() {
        legacy.putString(SESSION_KEY, "{\"refresh_token\":\"legacy\"}")
        secure.putString(SESSION_KEY, "{\"refresh_token\":\"secure\"}")

        moveSessionToSecureStore(legacy, secure)

        assertNull(legacy.getStringOrNull(SESSION_KEY))
    }

    @Test
    fun `a legacy store holding no session writes nothing to the secure store`() {
        secure.putString(SESSION_KEY, "{\"refresh_token\":\"secure\"}")

        moveSessionToSecureStore(legacy, secure)

        assertEquals("{\"refresh_token\":\"secure\"}", secure.getStringOrNull(SESSION_KEY))
    }

    @Test
    fun `a legacy store holding no session leaves an empty secure store empty`() {
        moveSessionToSecureStore(legacy, secure)

        assertEquals(emptySet(), secure.keys)
    }

    @Test
    fun `a secure store that rejects the write keeps the cleartext session`() {
        legacy.putString(SESSION_KEY, "{\"refresh_token\":\"legacy\"}")

        assertFailsWith<IllegalStateException> { moveSessionToSecureStore(legacy, UnwritableSettings()) }

        assertEquals("{\"refresh_token\":\"legacy\"}", legacy.getStringOrNull(SESSION_KEY))
    }

    @Test
    fun `every key but the session stays where it was`() {
        legacy.putString(SESSION_KEY, "{\"refresh_token\":\"legacy\"}")
        legacy.putString("onboarding_seen", "true")
        secure.putString("unrelated", "kept")

        moveSessionToSecureStore(legacy, secure)

        assertEquals("true", legacy.getStringOrNull("onboarding_seen"))
        assertEquals("kept", secure.getStringOrNull("unrelated"))
        assertEquals(setOf(SESSION_KEY, "unrelated"), secure.keys)
    }
}

// The Keychain refusing a write: KeychainSettings raises error() on any OSStatus it does not
// whitelist. Nothing else in Settings can fail, which is why only putString is overridden.
private class UnwritableSettings(private val delegate: Settings = MapSettings()) : Settings by delegate {
    override fun putString(key: String, value: String): Unit = error("Keychain error -34018")
}
