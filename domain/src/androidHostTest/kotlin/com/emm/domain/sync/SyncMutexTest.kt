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
import java.util.concurrent.atomic.AtomicInteger
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
    // A loaded machine can turn more acquisitions into Busy, including a spurious one on a free lock,
    // but isFree()'s independent probes below make one pause defeating all of them implausible.
    @Test
    fun `a release racing the deadline never leaves the lock held by the caller that gave up`() =
        runBlocking(Dispatchers.Default) {
            // The leak needs a release and a deadline on two cores at once. Measured, a
            // single-threaded dispatcher never reproduces it while the assertions below still pass —
            // so the precondition is asserted rather than the outcome, which cannot detect this.
            assertTrue(
                Runtime.getRuntime().availableProcessors() >= MIN_RACE_CORES,
                "this test needs $MIN_RACE_CORES cores to interleave a release with a deadline; " +
                    "on fewer it passes without ever sampling the race it exists to catch",
            )
            val waiterBusyCount = AtomicInteger(0)
            val waiterAcquiredCount = AtomicInteger(0)
            repeat(RACE_ROUNDS) {
                val raced: List<SyncMutex> = List(RACE_WIDTH) { SyncMutex(RACE_TIMEOUT) }
                raced.flatMap { syncMutex ->
                    listOf(
                        launch {
                            try {
                                syncMutex.withLock { delay(RACE_TIMEOUT) }
                            } catch (ignored: DomainException.Busy) {
                            }
                        },
                        launch {
                            try {
                                syncMutex.withLock { }
                                waiterAcquiredCount.incrementAndGet()
                            } catch (ignored: DomainException.Busy) {
                                waiterBusyCount.incrementAndGet()
                            }
                        },
                    )
                }.joinAll()
                raced.forEach { syncMutex ->
                    assertTrue(syncMutex.isFree(), "a caller that gave up walked off holding the lock")
                }
            }
            assertTrue(
                waiterBusyCount.get() > 0,
                "no Busy was ever observed for the waiter across $RACE_ROUNDS rounds — the deadline " +
                    "never landed before a release, so this test could not have caught a leaked lock",
            )
            assertTrue(
                waiterAcquiredCount.get() > 0,
                "no clean acquire was ever observed for the waiter across $RACE_ROUNDS rounds — the " +
                    "release never landed before the deadline, so this test could not have caught a leaked lock",
            )
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

        // The mutant this test guards against died at 20-25% of a 500-round budget at both
        // Dispatchers.Default and limitedParallelism(2); 200 keeps a margin over that kill point.
        const val RACE_ROUNDS = 200
        const val RACE_WIDTH = 200
        const val MIN_RACE_CORES = 2
    }
}

// A leaked lock never frees, so it alone explains refusal across every one of the LEAK_PROBE_ATTEMPTS
// independent probes below; no single stop-the-world pause spans all of them.
private const val LEAK_PROBE_ATTEMPTS = 50

private suspend fun SyncMutex.isFree(): Boolean {
    repeat(LEAK_PROBE_ATTEMPTS) { if (acquires()) return true }
    return false
}

private suspend fun SyncMutex.acquires(): Boolean = try {
    withLock { true }
} catch (ignored: DomainException.Busy) {
    false
}
