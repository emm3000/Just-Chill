package com.emm.domain.shared.backup

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The dirtiness predicate, directly — every input class in its truth table.
 *
 * It is the gate deciding whether an automatic backup cycle runs at all (`BackupOrchestrator
 * .isBackupDue`) **and** the second half of the staleness warning, so it has two callers that must
 * never disagree. It reached that position by extraction from a private function, and for one commit
 * its only safety was that the extracted text was identical to the original: mutating its `>` to
 * `>=` left the whole 34-test orchestrator suite green, because nothing there stands on the boundary.
 * Those are exactly the cases below.
 */
class HasLocalChangesSinceTest {

    private companion object {
        const val T: Long = 1_785_856_445_000L
    }

    @Test
    fun `an empty database is never dirty, even with no backup ever taken`() {
        // null latestLocalChangeAt = not one row across the four backed-up tables. SQLDelight types
        // the column Long?, so this is structurally distinct from 0 and must not be read as an
        // ancient change.
        assertFalse(hasLocalChangesSince(latestLocalChangeAt = null, lastSuccessfulBackupAt = null))
        assertFalse(hasLocalChangesSince(latestLocalChangeAt = null, lastSuccessfulBackupAt = T))
    }

    @Test
    fun `a device that has never backed up is dirty as soon as it holds anything`() {
        assertTrue(hasLocalChangesSince(latestLocalChangeAt = T, lastSuccessfulBackupAt = null))
        // Including a row written at the epoch — presence is what matters, not recency.
        assertTrue(hasLocalChangesSince(latestLocalChangeAt = 0L, lastSuccessfulBackupAt = null))
    }

    @Test
    fun `a change after the last backup is dirty`() {
        assertTrue(hasLocalChangesSince(latestLocalChangeAt = T + 1, lastSuccessfulBackupAt = T))
    }

    /**
     * The boundary, and the one a `>` → `>=` mutation moves. Equality means the snapshot was taken
     * at (or after) the newest write, so it already holds it: re-uploading would be a cycle that
     * changes nothing, every trigger, forever.
     */
    @Test
    fun `a change at the exact instant of the last backup is NOT dirty`() {
        assertFalse(hasLocalChangesSince(latestLocalChangeAt = T, lastSuccessfulBackupAt = T))
    }

    @Test
    fun `a change before the last backup is not dirty`() {
        assertFalse(hasLocalChangesSince(latestLocalChangeAt = T - 1, lastSuccessfulBackupAt = T))
    }
}
