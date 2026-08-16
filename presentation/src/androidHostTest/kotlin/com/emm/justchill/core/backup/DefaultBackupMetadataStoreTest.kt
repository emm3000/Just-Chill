package com.emm.justchill.core.backup

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupFailureState
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

    // ── The failure streak (ADR 009 Phase 3, unit 3a-ii) ─────────────────────────────

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

        // Returned AND stored: a caller that trusted only the return value would still be right, and
        // a screen opened later reads the same thing.
        assertEquals(BackupFailureState(2, BackupFailureReason.Unverified), store.failureState("user-a"))
    }

    @Test
    fun `clearFailures resets the streak and drops the reason with it`() {
        store.recordFailure("user-a", BackupFailureReason.Serialization)
        store.recordFailure("user-a", BackupFailureReason.Serialization)

        store.clearFailures("user-a")

        // Not just the count: a reason left behind would label the NEXT outage with the last one's.
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

    /**
     * The reason the streak is persisted at all. A failure indicator that a restart resets tells a
     * device whose backups have been failing all week that everything is fine — and this is the only
     * test that can fail if the two values ever move into a field.
     *
     * A second [AppPreferences] and a second store over the SAME [MapSettings] is what process death
     * looks like from here: the objects are gone, the key-value backing is not.
     */
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

    /**
     * Account deletion wipes the streak too, for the same reason it wipes the watermark: the same
     * device registering again would otherwise open on a brand-new account already showing somebody
     * else's failures.
     */
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
