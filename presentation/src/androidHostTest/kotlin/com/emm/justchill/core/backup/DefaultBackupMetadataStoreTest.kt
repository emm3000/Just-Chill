package com.emm.justchill.core.backup

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupFailureState
import com.emm.justchill.core.preferences.AppPreferences
import com.russhwolf.settings.MapSettings
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun `a user who has never failed has no streak and no reason`() {
        assertEquals(BackupFailureState.None, store.failureState("user-a"))
    }

    @Test
    fun `recordFailure increments the streak and keeps the newest reason`() {
        assertEquals(
            BackupFailureState(1, BackupFailureReason.Network),
            store.recordFailure("user-a", BackupFailureReason.Network),
        )
        assertEquals(
            BackupFailureState(2, BackupFailureReason.Unverified),
            store.recordFailure("user-a", BackupFailureReason.Unverified),
        )

        assertEquals(BackupFailureState(2, BackupFailureReason.Unverified), store.failureState("user-a"))
    }

    @Test
    fun `clearFailures resets the streak and drops the reason with it`() {
        store.recordFailure("user-a", BackupFailureReason.Serialization)
        store.recordFailure("user-a", BackupFailureReason.Serialization)

        store.clearFailures("user-a")

        assertEquals(BackupFailureState.None, store.failureState("user-a"))
    }

    @Test
    fun `each user's streak is isolated from every other user's`() {
        store.recordFailure("user-a", BackupFailureReason.Network)
        store.recordFailure("user-a", BackupFailureReason.Network)
        store.recordFailure("user-b", BackupFailureReason.LocalDatabase)

        assertEquals(BackupFailureState(2, BackupFailureReason.Network), store.failureState("user-a"))
        assertEquals(BackupFailureState(1, BackupFailureReason.LocalDatabase), store.failureState("user-b"))

        store.clearFailures("user-a")
        assertEquals(BackupFailureState(1, BackupFailureReason.LocalDatabase), store.failureState("user-b"))
    }

    @Test
    fun `the streak survives a fresh store built over the same settings`() {
        val settings = MapSettings()
        DefaultBackupMetadataStore(AppPreferences(settings)).apply {
            setLastSuccessfulBackupAt("user-a", 1_755_000_000_000L)
            recordFailure("user-a", BackupFailureReason.Unauthorized)
            recordFailure("user-a", BackupFailureReason.Network)
        }

        val afterRestart = DefaultBackupMetadataStore(AppPreferences(settings))

        assertEquals(BackupFailureState(2, BackupFailureReason.Network), afterRestart.failureState("user-a"))
        assertEquals(1_755_000_000_000L, afterRestart.lastSuccessfulBackupAt("user-a"))
    }

    @Test
    fun `clear removes the streak as well as the watermark, and only for that user`() {
        store.setLastSuccessfulBackupAt("user-a", 1_755_000_000_000L)
        store.recordFailure("user-a", BackupFailureReason.Serialization)
        store.recordFailure("user-b", BackupFailureReason.Network)

        store.clear("user-a")

        assertNull(store.lastSuccessfulBackupAt("user-a"))
        assertEquals(BackupFailureState.None, store.failureState("user-a"))
        assertEquals(BackupFailureState(1, BackupFailureReason.Network), store.failureState("user-b"))
    }
}
