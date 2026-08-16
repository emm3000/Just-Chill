package com.emm.justchill.core.preferences

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupFailureState
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

    // ── The backup failure streak (ADR 009 Phase 3, unit 3a-ii) ─────────────────────

    @Test
    fun `backupFailure is None when nothing was ever written`() {
        assertEquals(BackupFailureState.None, prefs.backupFailure("user-a"))
    }

    @Test
    fun `backupFailure round-trips both halves of what was set`() {
        prefs.setBackupFailure("user-a", BackupFailureState(3, BackupFailureReason.Unverified))

        assertEquals(BackupFailureState(3, BackupFailureReason.Unverified), prefs.backupFailure("user-a"))
    }

    /**
     * The reason is stored by enum NAME, so this is also the test that fails if the persisted form
     * ever quietly becomes the ordinal — reordering the enum would then rewrite history on every
     * device that already had a value.
     */
    @Test
    fun `the reason is persisted under its own name`() {
        prefs.setBackupFailure("user-a", BackupFailureState(1, BackupFailureReason.LocalDatabase))

        assertEquals(BackupFailureReason.LocalDatabase, prefs.backupFailure("user-a").lastReason)
    }

    @Test
    fun `writing a state with no reason removes the stored one rather than leaving it behind`() {
        prefs.setBackupFailure("user-a", BackupFailureState(2, BackupFailureReason.Network))

        prefs.setBackupFailure("user-a", BackupFailureState.None)

        assertEquals(BackupFailureState.None, prefs.backupFailure("user-a"))
    }

    @Test
    fun `backupFailure is isolated per user`() {
        prefs.setBackupFailure("user-a", BackupFailureState(1, BackupFailureReason.Network))

        assertEquals(BackupFailureState.None, prefs.backupFailure("user-b"))
    }

    @Test
    fun `clearBackupMetadata removes the failure streak too, and only for that user`() {
        prefs.setBackupFailure("user-a", BackupFailureState(4, BackupFailureReason.Serialization))
        prefs.setBackupFailure("user-b", BackupFailureState(1, BackupFailureReason.Network))
        prefs.setLastSyncedAt("user-a", 300L)

        prefs.clearBackupMetadata("user-a")

        // A deleted account must not leave a failure count for whoever registers on this device next.
        assertEquals(BackupFailureState.None, prefs.backupFailure("user-a"))
        assertEquals(BackupFailureState(1, BackupFailureReason.Network), prefs.backupFailure("user-b"))
        assertEquals(300L, prefs.lastSyncedAt("user-a"))
    }

    /**
     * The three per-user prefixes became one `userKey(prefix, userId)` helper in 3a-ii. Two users
     * whose ids are prefixes of one another is what a naive concatenation gets wrong, and every key
     * in this class is built the same way — so proving it once here covers all five.
     */
    @Test
    fun `keys built for one user never collide with another whose id extends it`() {
        prefs.setLastSuccessfulBackupAt("user", 100L)
        prefs.setLastSuccessfulBackupAt("user-a", 200L)
        prefs.setBackupFailure("user", BackupFailureState(1, BackupFailureReason.Network))
        prefs.setBackupFailure("user-a", BackupFailureState(2, BackupFailureReason.Unverified))

        assertEquals(100L, prefs.lastSuccessfulBackupAt("user"))
        assertEquals(200L, prefs.lastSuccessfulBackupAt("user-a"))
        assertEquals(BackupFailureState(1, BackupFailureReason.Network), prefs.backupFailure("user"))
        assertEquals(BackupFailureState(2, BackupFailureReason.Unverified), prefs.backupFailure("user-a"))
    }
}
