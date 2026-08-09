package com.emm.domain.sync

import org.junit.Test
import kotlin.test.assertEquals

class ConflictResolverTest {

    private val resolver = ConflictResolver()

    private fun pending(updatedAt: Long) = LocalRevision(updatedAt = updatedAt, hasUnpushedEdit = true)

    private fun synced(updatedAt: Long) = LocalRevision(updatedAt = updatedAt, hasUnpushedEdit = false)

    // ---------------------------------------------------------------------------
    // Null local (no local row exists) → always apply remote
    // ---------------------------------------------------------------------------

    @Test
    fun `no local row always applies remote`() {
        val result = resolver.resolve(local = null, remoteUpdatedAt = 1000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `no local row applies remote even when remote is zero`() {
        val result = resolver.resolve(local = null, remoteUpdatedAt = 0L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    // ---------------------------------------------------------------------------
    // Already-synced local row → apply remote, whatever the clocks say.
    //
    // This is the fix for the resurrection loop: a device with a fast clock used to keep and
    // re-push its own already-synced copy forever, silently overwriting the other device's
    // newer edit on every cycle.
    // ---------------------------------------------------------------------------

    @Test
    fun `synced local row applies remote even when its timestamp is newer`() {
        val result = resolver.resolve(local = synced(updatedAt = 9_999L), remoteUpdatedAt = 1000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `synced local row applies remote when timestamps are equal`() {
        val result = resolver.resolve(local = synced(updatedAt = 1000L), remoteUpdatedAt = 1000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `synced local row applies remote when remote is newer`() {
        val result = resolver.resolve(local = synced(updatedAt = 1000L), remoteUpdatedAt = 2000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `a synced row far in the future still loses to the server`() {
        // A clock two hours ahead writes timestamps two hours ahead. With nothing unpushed
        // locally there is no edit to protect, so the skew must not buy the row a win.
        val twoHoursAhead = 7_200_000L
        val result = resolver.resolve(local = synced(updatedAt = twoHoursAhead), remoteUpdatedAt = 1L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    // ---------------------------------------------------------------------------
    // Unpushed local edit → a real conflict, decided by client updatedAt (LWW).
    // ---------------------------------------------------------------------------

    @Test
    fun `remote newer than an unpushed local edit applies remote`() {
        val result = resolver.resolve(local = pending(1000L), remoteUpdatedAt = 2000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `remote one millisecond newer applies remote`() {
        val result = resolver.resolve(local = pending(999L), remoteUpdatedAt = 1000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `unpushed local edit newer than remote keeps local`() {
        val result = resolver.resolve(local = pending(2000L), remoteUpdatedAt = 1000L)
        assertEquals(Resolution.KeepLocal, result)
    }

    @Test
    fun `local one millisecond newer keeps local`() {
        val result = resolver.resolve(local = pending(1001L), remoteUpdatedAt = 1000L)
        assertEquals(Resolution.KeepLocal, result)
    }

    // ---------------------------------------------------------------------------
    // Equal timestamps → deterministic remote-wins tie-break
    // ---------------------------------------------------------------------------

    @Test
    fun `equal timestamps applies remote as tie-break`() {
        val result = resolver.resolve(local = pending(1000L), remoteUpdatedAt = 1000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `equal zero timestamps applies remote as tie-break`() {
        val result = resolver.resolve(local = pending(0L), remoteUpdatedAt = 0L)
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
        val result = resolver.resolve(local = pending(2000L), remoteUpdatedAt = 3000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `newer local edit beats older remote tombstone keeps local`() {
        // Local last edited at T=3000, remote deleted at T=2000 (tombstone).
        // Local updatedAt (3000) > remote updatedAt (2000) → KeepLocal.
        val result = resolver.resolve(local = pending(3000L), remoteUpdatedAt = 2000L)
        assertEquals(Resolution.KeepLocal, result)
    }

    @Test
    fun `a synced local row does not resist a remote tombstone`() {
        // Deleting on device A and never touching the row on device B must delete it on B,
        // even if B's clock stamped the row later than A's delete.
        val result = resolver.resolve(local = synced(3000L), remoteUpdatedAt = 2000L)
        assertEquals(Resolution.ApplyRemote, result)
    }
}
