package com.emm.domain.sync

import org.junit.Test
import kotlin.test.assertEquals

class ConflictResolverTest {

    private val resolver = ConflictResolver()

    private fun pending(updatedAt: Long) = LocalRevision(updatedAt = updatedAt, hasUnpushedEdit = true)

    private fun synced(updatedAt: Long) = LocalRevision(updatedAt = updatedAt, hasUnpushedEdit = false)

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
        val twoHoursAhead = 7_200_000L
        val result = resolver.resolve(local = synced(updatedAt = twoHoursAhead), remoteUpdatedAt = 1L)
        assertEquals(Resolution.ApplyRemote, result)
    }

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

    @Test
    fun `newer tombstone beats older local edit applies remote`() {
        val result = resolver.resolve(local = pending(2000L), remoteUpdatedAt = 3000L)
        assertEquals(Resolution.ApplyRemote, result)
    }

    @Test
    fun `newer local edit beats older remote tombstone keeps local`() {
        val result = resolver.resolve(local = pending(3000L), remoteUpdatedAt = 2000L)
        assertEquals(Resolution.KeepLocal, result)
    }

    @Test
    fun `a synced local row does not resist a remote tombstone`() {
        val result = resolver.resolve(local = synced(3000L), remoteUpdatedAt = 2000L)
        assertEquals(Resolution.ApplyRemote, result)
    }
}
