package com.emm.domain.shared.backup

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasLocalChangesSinceTest {

    private companion object {
        const val T: Long = 1_785_856_445_000L
    }

    @Test
    fun `an empty database is never dirty, even with no backup ever taken`() {
        assertFalse(hasLocalChangesSince(latestLocalChangeAt = null, lastSuccessfulBackupAt = null))
        assertFalse(hasLocalChangesSince(latestLocalChangeAt = null, lastSuccessfulBackupAt = T))
    }

    @Test
    fun `a device that has never backed up is dirty as soon as it holds anything`() {
        assertTrue(hasLocalChangesSince(latestLocalChangeAt = T, lastSuccessfulBackupAt = null))
        assertTrue(hasLocalChangesSince(latestLocalChangeAt = 0L, lastSuccessfulBackupAt = null))
    }

    @Test
    fun `a change after the last backup is dirty`() {
        assertTrue(hasLocalChangesSince(latestLocalChangeAt = T + 1, lastSuccessfulBackupAt = T))
    }

    @Test
    fun `a change at the exact instant of the last backup is NOT dirty`() {
        assertFalse(hasLocalChangesSince(latestLocalChangeAt = T, lastSuccessfulBackupAt = T))
    }

    @Test
    fun `a change before the last backup is not dirty`() {
        assertFalse(hasLocalChangesSince(latestLocalChangeAt = T - 1, lastSuccessfulBackupAt = T))
    }
}
