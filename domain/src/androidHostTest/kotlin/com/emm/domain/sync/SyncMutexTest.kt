package com.emm.domain.sync

import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class SyncMutexTest {

    private val syncMutex = SyncMutex()

    @Test
    fun `a holder that never returns fails the next caller instead of parking it forever`() = runTest {
        val stuck = CompletableDeferred<Unit>()
        val holder = launch { syncMutex.withLock { stuck.await() } }
        testScheduler.runCurrent()
        var blockRan = false

        assertFailsWith<DomainException.Busy> { syncMutex.withLock { blockRan = true } }

        assertFalse(blockRan, "the block must not run for a caller that never got the lock")
        stuck.complete(Unit)
        holder.join()
    }

    @Test
    fun `the timeout bounds the wait for the lock, never the work done while holding it`() = runTest {
        val heldFor = 10.minutes

        val elapsed: Long = syncMutex.withLock {
            val startedAt: Long = testScheduler.currentTime
            delay(heldFor)
            testScheduler.currentTime - startedAt
        }

        assertEquals(heldFor.inWholeMilliseconds, elapsed, "work under the lock must not be cut short")
    }

    @Test
    fun `a caller that timed out took nothing, so the lock frees when the holder returns`() = runTest {
        val stuck = CompletableDeferred<Unit>()
        val holder = launch { syncMutex.withLock { stuck.await() } }
        testScheduler.runCurrent()

        assertFailsWith<DomainException.Busy> { syncMutex.withLock { } }

        stuck.complete(Unit)
        holder.join()
        assertTrue(syncMutex.withLock { true }, "the lock must be free once the holder returns")
    }

    @Test
    fun `a block that throws still releases the lock`() = runTest {
        assertFailsWith<IllegalStateException> { syncMutex.withLock { error("boom") } }

        assertTrue(syncMutex.withLock { true }, "the lock must be free after a failed block")
    }

    // Real threads, not runTest: this needs the holder's release and the acquisition deadline to land
    // on two threads within the same nanoseconds, and virtual time orders every event on one thread.
    // A loaded machine can only turn more acquisitions into Busy, never into the leak asserted here.
    @Test
    fun `a release racing the deadline never leaves the lock held by the caller that gave up`() =
        runBlocking(Dispatchers.Default) {
            repeat(RACE_ROUNDS) {
                val raced: List<SyncMutex> = List(RACE_WIDTH) { SyncMutex(RACE_TIMEOUT) }
                raced.flatMap { syncMutex ->
                    listOf(
                        launch { runCatching { syncMutex.withLock { delay(RACE_TIMEOUT) } } },
                        launch { runCatching { syncMutex.withLock { } } },
                    )
                }.joinAll()
                raced.forEach { syncMutex ->
                    assertTrue(syncMutex.isFree(), "a caller that gave up walked off holding the lock")
                }
            }
        }

    @Test
    fun `cancelling a waiting caller surfaces as cancellation and leaks no lock`() = runTest {
        val stuck = CompletableDeferred<Unit>()
        val holder = launch { syncMutex.withLock { stuck.await() } }
        testScheduler.runCurrent()
        var failure: DomainException? = null

        val waiter = launch {
            try {
                syncMutex.withLock { }
            } catch (e: DomainException) {
                failure = e
            }
        }
        testScheduler.runCurrent()
        waiter.cancelAndJoin()

        assertNull(failure, "cancellation must propagate as cancellation, got $failure")
        stuck.complete(Unit)
        holder.join()
        assertTrue(syncMutex.withLock { true }, "a cancelled waiter must not hold the lock")
    }

    private companion object {
        val RACE_TIMEOUT = 2.milliseconds
        const val RACE_ROUNDS = 500
        const val RACE_WIDTH = 200
    }
}

// A leaked lock never frees, so a second refusal cannot be the scheduler running late.
private suspend fun SyncMutex.isFree(): Boolean = acquires() || acquires()

private suspend fun SyncMutex.acquires(): Boolean = try {
    withLock { true }
} catch (ignored: DomainException.Busy) {
    false
}
