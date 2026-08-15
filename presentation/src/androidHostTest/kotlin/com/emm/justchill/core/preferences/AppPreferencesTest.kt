package com.emm.justchill.core.preferences

import com.russhwolf.settings.MapSettings
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [AppPreferences] against [MapSettings] — the in-memory [com.russhwolf.settings.Settings] double
 * already wired for the whole Koin graph in `TestPlatformModule`. No key needs a real Android
 * SharedPreferences or iOS NSUserDefaults backend to prove its round-trip, its per-user isolation,
 * or its sentinel handling.
 */
class AppPreferencesTest {

    private lateinit var prefs: AppPreferences

    @Before
    fun setUp() {
        prefs = AppPreferences(MapSettings())
    }

    @Test
    fun `lastSuccessfulBackupAt is null when never written`() {
        assertNull(prefs.lastSuccessfulBackupAt("user-a"))
    }

    @Test
    fun `lastSuccessfulBackupAt round-trips what was set`() {
        prefs.setLastSuccessfulBackupAt("user-a", 1_755_000_000_000L)

        assertEquals(1_755_000_000_000L, prefs.lastSuccessfulBackupAt("user-a"))
    }

    @Test
    fun `lastSuccessfulBackupAt is isolated per user`() {
        prefs.setLastSuccessfulBackupAt("user-a", 100L)

        assertNull(prefs.lastSuccessfulBackupAt("user-b"))
        assertEquals(100L, prefs.lastSuccessfulBackupAt("user-a"))
    }

    @Test
    fun `clearBackupMetadata removes only the backup timestamp for that user`() {
        prefs.setLastSuccessfulBackupAt("user-a", 100L)
        prefs.setLastSuccessfulBackupAt("user-b", 200L)
        prefs.setLastSyncedAt("user-a", 300L)

        prefs.clearBackupMetadata("user-a")

        assertNull(prefs.lastSuccessfulBackupAt("user-a"))
        // A different user's timestamp, and this same user's unrelated sync key, must survive —
        // clearBackupMetadata is deliberately narrower than clearSyncMetadata.
        assertEquals(200L, prefs.lastSuccessfulBackupAt("user-b"))
        assertEquals(300L, prefs.lastSyncedAt("user-a"))
    }
}
