package com.emm.justchill.core.backup

import com.emm.justchill.core.preferences.AppPreferences
import com.russhwolf.settings.MapSettings
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [DefaultBackupMetadataStore] against a real [AppPreferences] over [MapSettings].
 *
 * ADR 009 2c-iii-a moved the backup-watermark clear off `DefaultSyncCursorStore.clear` onto this
 * seam. `DefaultSyncCursorStoreTest` now pins that `clear` no longer touches the watermark; this
 * suite is the corresponding positive coverage — round trip, per-user isolation, and clear — for
 * the seam that replaced it.
 */
class DefaultBackupMetadataStoreTest {

    private lateinit var prefs: AppPreferences
    private lateinit var store: DefaultBackupMetadataStore

    @Before
    fun setUp() {
        prefs = AppPreferences(MapSettings())
        store = DefaultBackupMetadataStore(prefs)
    }

    @Test
    fun `never backed up returns null`() {
        assertNull(store.lastSuccessfulBackupAt("user-a"))
    }

    @Test
    fun `setLastSuccessfulBackupAt then lastSuccessfulBackupAt round-trips the same value`() {
        val userId = "user-a"

        store.setLastSuccessfulBackupAt(userId, 1_755_000_000_000L)

        assertEquals(1_755_000_000_000L, store.lastSuccessfulBackupAt(userId))
    }

    @Test
    fun `each user's watermark is isolated from every other user's`() {
        store.setLastSuccessfulBackupAt("user-a", 1_755_000_000_000L)
        store.setLastSuccessfulBackupAt("user-b", 1_800_000_000_000L)

        assertEquals(1_755_000_000_000L, store.lastSuccessfulBackupAt("user-a"))
        assertEquals(1_800_000_000_000L, store.lastSuccessfulBackupAt("user-b"))
    }

    @Test
    fun `clear removes only the cleared user's watermark`() {
        store.setLastSuccessfulBackupAt("user-a", 1_755_000_000_000L)
        store.setLastSuccessfulBackupAt("user-b", 1_800_000_000_000L)

        store.clear("user-a")

        assertNull(store.lastSuccessfulBackupAt("user-a"))
        assertEquals(1_800_000_000_000L, store.lastSuccessfulBackupAt("user-b"))
    }
}
