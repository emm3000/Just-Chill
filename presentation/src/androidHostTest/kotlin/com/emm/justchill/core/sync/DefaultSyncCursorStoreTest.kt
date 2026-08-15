package com.emm.justchill.core.sync

import com.emm.justchill.core.preferences.AppPreferences
import com.russhwolf.settings.MapSettings
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [DefaultSyncCursorStore.clear] against a real [AppPreferences] over [MapSettings] — proves the
 * WIRING, not just each half in isolation.
 *
 * `clear` no longer touches the backup watermark: ADR 009 2c-iii-a moved that clear onto its own
 * seam (`BackupMetadataStore`, `DefaultBackupMetadataStoreTest`), called as its own step by
 * `DeleteUserAccountUseCase` rather than riding this one. This test now pins the narrower claim —
 * `clear` empties only the two genuine sync keys — so a "widen it back onto this class" regression
 * would turn it red instead of silently reintroducing the coupling 2c-iii-a removed.
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
    fun `clear removes the pull cursor and the last-synced-at timestamp, and leaves the backup watermark alone`() {
        val userId = "user-a"
        prefs.setLastPulledAt(userId, "2026-08-14T00:00:00Z")
        prefs.setLastSyncedAt(userId, 1_755_000_000_000L)
        prefs.setLastSuccessfulBackupAt(userId, 1_755_000_000_000L)

        store.clear(userId)

        assertNull(prefs.lastPulledAt(userId))
        assertNull(prefs.lastSyncedAt(userId))
        assertEquals(1_755_000_000_000L, prefs.lastSuccessfulBackupAt(userId))
    }
}
