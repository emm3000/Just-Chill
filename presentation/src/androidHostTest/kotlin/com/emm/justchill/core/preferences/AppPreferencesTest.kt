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
    fun `writing a state with no reason clears the previous one rather than leaving it attached`() {
        prefs.setBackupFailure("user-a", BackupFailureState(2, BackupFailureReason.Network))

        prefs.setBackupFailure("user-a", BackupFailureState.None)

        assertEquals(BackupFailureState.None, prefs.backupFailure("user-a"))
    }

    /**
     * **The torn-write fix, stated structurally.** The count and the reason used to be two keys, and
     * `Settings` has no transaction: two writes are two commits, so a process killed between them
     * left a streak carrying the previous outage's reason — or a reason with no streak. One key, one
     * commit, and the half-written pair stops being representable on disk.
     *
     * Asserted over the backing map rather than through the round-trip above, because a round-trip
     * passes just as happily with two keys. This is the only test that can see the difference.
     */
    @Test
    fun `both halves of the streak are stored under a single key`() {
        val settings = MapSettings()

        AppPreferences(settings).setBackupFailure("user-a", BackupFailureState(3, BackupFailureReason.Network))

        assertEquals(1, settings.keys.size, "The streak must be one key, not a pair: ${settings.keys}")
    }

    /**
     * A value this build cannot parse — an older spelling, a truncation, anything — degrades to
     * [BackupFailureState.None]. It must never throw: this is the read that runs while the app is
     * trying to REPORT a backup failure, and crashing there loses the failure and the app with it.
     */
    @Test
    fun `an unparseable stored value reads as no failure at all`() {
        val settings = MapSettings()
        val prefsOverRawSettings = AppPreferences(settings)
        prefsOverRawSettings.setBackupFailure("user-a", BackupFailureState(3, BackupFailureReason.Network))
        val key = settings.keys.single()

        settings.putString(key, "not-a-streak")
        assertEquals(BackupFailureState.None, prefsOverRawSettings.backupFailure("user-a"))

        // An unparseable count discards the whole reading, reason included. Parsing the two halves
        // independently would answer (0, Network) here — a reason hanging off a streak of zero, the
        // exact disagreeing pair the single-key encoding exists to make impossible.
        settings.putString(key, "abc|Network")
        assertEquals(BackupFailureState.None, prefsOverRawSettings.backupFailure("user-a"))

        // Same rule for a count no Int can hold.
        settings.putString(key, "99999999999999|Network")
        assertEquals(BackupFailureState.None, prefsOverRawSettings.backupFailure("user-a"))

        // A count with a reason this build no longer has a name for keeps the count — the streak is
        // the fact a UI warns on, and losing it because a label was renamed would hide a broken
        // device. See BackupHealth: (5, null) is a real value and 3b must handle it.
        settings.putString(key, "5|HashMismatch")
        assertEquals(BackupFailureState(5, null), prefsOverRawSettings.backupFailure("user-a"))
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
     * in this class is built the same way — so proving it once here covers all four.
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
