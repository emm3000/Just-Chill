package com.emm.justchill.core.backup

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupFailureState
import com.russhwolf.settings.MapSettings
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultBackupMetadataStoreTest {

    private lateinit var settings: MapSettings
    private lateinit var store: DefaultBackupMetadataStore

    @Before
    fun setUp() {
        settings = MapSettings()
        store = DefaultBackupMetadataStore(settings)
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
    fun `clear leaves keys this store does not own alone`() {
        settings.putLong("last_synced_at_user-a", 300L)
        store.setLastSuccessfulBackupAt("user-a", 100L)

        store.clear("user-a")

        assertEquals(300L, settings.getLong("last_synced_at_user-a", -1L))
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

    /**
     * The reason is stored by enum NAME, so this is also the test that fails if the persisted form
     * ever quietly becomes the ordinal — reordering the enum would then rewrite history on every
     * device that already had a value.
     */
    @Test
    fun `the reason is persisted under its own name`() {
        store.recordFailure("user-a", BackupFailureReason.LocalDatabase)

        assertEquals("1|LocalDatabase", settings.getStringOrNull(settings.keys.single()))
    }

    @Test
    fun `clearFailures resets the streak and drops the reason with it`() {
        store.recordFailure("user-a", BackupFailureReason.Serialization)
        store.recordFailure("user-a", BackupFailureReason.Serialization)

        store.clearFailures("user-a")

        assertEquals(BackupFailureState.None, store.failureState("user-a"))
    }

    /**
     * **The torn-write fix, stated structurally.** The count and the reason used to be two keys, and
     * `Settings` has no transaction: two writes are two commits, so a process killed between them
     * left a streak carrying the previous outage's reason — or a reason with no streak. One key, one
     * commit, and the half-written pair stops being representable on disk.
     *
     * Asserted over the backing map rather than through a round-trip, because a round-trip passes
     * just as happily with two keys. This is the only test that can see the difference.
     */
    @Test
    fun `both halves of the streak are stored under a single key`() {
        store.recordFailure("user-a", BackupFailureReason.Network)

        assertEquals(1, settings.keys.size, "The streak must be one key, not a pair: ${settings.keys}")
    }

    /**
     * A value this build cannot parse — an older spelling, a truncation, anything — degrades to
     * [BackupFailureState.None]. It must never throw: this is the read that runs while the app is
     * trying to REPORT a backup failure, and crashing there loses the failure and the app with it.
     */
    @Test
    fun `an unparseable stored value reads as no failure at all`() {
        store.recordFailure("user-a", BackupFailureReason.Network)
        val key = settings.keys.single()

        settings.putString(key, "not-a-streak")
        assertEquals(BackupFailureState.None, store.failureState("user-a"))

        // An unparseable count discards the whole reading, reason included. Parsing the two halves
        // independently would answer (0, Network) here — a reason hanging off a streak of zero, the
        // exact disagreeing pair the single-key encoding exists to make impossible.
        settings.putString(key, "abc|Network")
        assertEquals(BackupFailureState.None, store.failureState("user-a"))

        // Same rule for a count no Int can hold.
        settings.putString(key, "99999999999999|Network")
        assertEquals(BackupFailureState.None, store.failureState("user-a"))

        // A count with a reason this build no longer has a name for keeps the count — the streak is
        // the fact a UI warns on, and losing it because a label was renamed would hide a broken
        // device. See BackupHealth: (5, null) is a real value and 3b must handle it.
        settings.putString(key, "5|HashMismatch")
        assertEquals(BackupFailureState(5, null), store.failureState("user-a"))
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
        DefaultBackupMetadataStore(settings).apply {
            setLastSuccessfulBackupAt("user-a", 1_755_000_000_000L)
            recordFailure("user-a", BackupFailureReason.Unauthorized)
            recordFailure("user-a", BackupFailureReason.Network)
        }

        val afterRestart = DefaultBackupMetadataStore(settings)

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

    /**
     * Two users whose ids are prefixes of one another is what a naive concatenation gets wrong, and
     * every key in this class is built the same way — so proving it once covers all of them.
     */
    @Test
    fun `keys built for one user never collide with another whose id extends it`() {
        store.setLastSuccessfulBackupAt("user", 100L)
        store.setLastSuccessfulBackupAt("user-a", 200L)
        store.recordFailure("user", BackupFailureReason.Network)
        store.recordFailure("user-a", BackupFailureReason.Unverified)
        store.recordFailure("user-a", BackupFailureReason.Unverified)

        assertEquals(100L, store.lastSuccessfulBackupAt("user"))
        assertEquals(200L, store.lastSuccessfulBackupAt("user-a"))
        assertEquals(BackupFailureState(1, BackupFailureReason.Network), store.failureState("user"))
        assertEquals(BackupFailureState(2, BackupFailureReason.Unverified), store.failureState("user-a"))
    }
}
