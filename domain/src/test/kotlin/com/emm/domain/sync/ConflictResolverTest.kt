package com.emm.domain.sync

import org.junit.Test
import kotlin.test.assertEquals

class ConflictResolverTest {

    private val resolver = ConflictResolver()

    // ---------------------------------------------------------------------------
    // Null local (no local row exists) → always apply remote
    // ---------------------------------------------------------------------------

    @Test
    fun `no local row always applies remote`() {
        val result = resolver.resolve(localUpdatedAt = null, remoteUpdatedAt = 1000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `no local row applies remote even when remote is zero`() {
        val result = resolver.resolve(localUpdatedAt = null, remoteUpdatedAt = 0L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    // ---------------------------------------------------------------------------
    // Remote is newer → apply remote
    // ---------------------------------------------------------------------------

    @Test
    fun `remote newer than local applies remote`() {
        val result = resolver.resolve(localUpdatedAt = 1000L, remoteUpdatedAt = 2000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `remote one millisecond newer applies remote`() {
        val result = resolver.resolve(localUpdatedAt = 999L, remoteUpdatedAt = 1000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    // ---------------------------------------------------------------------------
    // Local is newer → keep local
    // ---------------------------------------------------------------------------

    @Test
    fun `local newer than remote keeps local`() {
        val result = resolver.resolve(localUpdatedAt = 2000L, remoteUpdatedAt = 1000L)
        assertEquals(Resolution.KeepLocal, result)
    }

    @Test
    fun `local one millisecond newer keeps local`() {
        val result = resolver.resolve(localUpdatedAt = 1001L, remoteUpdatedAt = 1000L)
        assertEquals(Resolution.KeepLocal, result)
    }

    // ---------------------------------------------------------------------------
    // Equal timestamps → deterministic remote-wins tie-break
    // ---------------------------------------------------------------------------

    @Test
    fun `equal timestamps applies remote as tie-break`() {
        val result = resolver.resolve(localUpdatedAt = 1000L, remoteUpdatedAt = 1000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `equal zero timestamps applies remote as tie-break`() {
        val result = resolver.resolve(localUpdatedAt = 0L, remoteUpdatedAt = 0L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    // ---------------------------------------------------------------------------
    // Tombstone vs live edit — modelled as updatedAt comparisons.
    // A soft-deleted row has a newer updatedAt when the delete happened after the edit,
    // and an older updatedAt when the edit happened after the delete.
    // ---------------------------------------------------------------------------

    @Test
    fun `newer tombstone beats older local edit applies remote`() {
        // Remote deleted the row at T=3000 (tombstone), local last edited at T=2000.
        // Remote updatedAt (3000) > local updatedAt (2000) → ApplyRemote.
        val result = resolver.resolve(localUpdatedAt = 2000L, remoteUpdatedAt = 3000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `newer local edit beats older remote tombstone keeps local`() {
        // Local last edited at T=3000, remote deleted at T=2000 (tombstone).
        // Local updatedAt (3000) > remote updatedAt (2000) → KeepLocal.
        val result = resolver.resolve(localUpdatedAt = 3000L, remoteUpdatedAt = 2000L)
        assertEquals(Resolution.KeepLocal, result)
    }
}
