package com.emm.justchill.core.sync

import com.emm.justchill.core.preferences.AppPreferences
import com.russhwolf.settings.MapSettings
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNull

/**
 * [DefaultSyncCursorStore.clear] against a real [AppPreferences] over [MapSettings] — proves the
 * WIRING, not just each half in isolation. `AppPreferencesTest` (sibling suite, `core/preferences/`)
 * already covers `AppPreferences.clearBackupMetadata` on its own; nothing asserted that
 * `DefaultSyncCursorStore.clear` actually calls it, alongside `clearSyncMetadata`, until this test.
 *
 * `clear` used to be `= prefs.clearSyncMetadata(userId)`, an expression body. It is a block body
 * now so it can also call `clearBackupMetadata`, and reverting it to a one-line expression body is
 * exactly the shape of a "simplify this" pass — the gate stays green either way, since detekt and
 * the compiler have no opinion on which lines a function body contains. This test is the only net.
 */
class DefaultSyncCursorStoreTest {

    private lateinit var prefs: AppPreferences
    private lateinit var store: DefaultSyncCursorStore

    @Before
    fun setUp() {
        prefs = AppPreferences(MapSettings())
        store = DefaultSyncCursorStore(prefs)
    }

    @Test
    fun `clear removes the pull cursor, the last-synced-at timestamp, and the backup watermark`() {
        val userId = "user-a"
        prefs.setLastPulledAt(userId, "2026-08-14T00:00:00Z")
        prefs.setLastSyncedAt(userId, 1_755_000_000_000L)
        prefs.setLastSuccessfulBackupAt(userId, 1_755_000_000_000L)

        store.clear(userId)

        assertNull(prefs.lastPulledAt(userId))
        assertNull(prefs.lastSyncedAt(userId))
        assertNull(prefs.lastSuccessfulBackupAt(userId))
    }
}
