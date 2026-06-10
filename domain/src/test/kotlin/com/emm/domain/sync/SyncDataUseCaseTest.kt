package com.emm.domain.sync

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncDataUseCaseTest {

    private val syncRepository = mockk<SyncRepository>()
    private val useCase = SyncDataUseCase(syncRepository)

    @Test
    fun `invoke delegates to repository once`() = runTest {
        coEvery { syncRepository.sync() } returns Unit

        useCase()

        coVerify(exactly = 1) { syncRepository.sync() }
    }

    @Test
    fun `repository failure propagates`() = runTest {
        coEvery { syncRepository.sync() } throws RuntimeException("network error")

        var caught: Throwable? = null
        try {
            useCase()
        } catch (e: RuntimeException) {
            caught = e
        }

        assert(caught != null) { "Expected exception to propagate" }
        coVerify(exactly = 1) { syncRepository.sync() }
    }

    // -------------------------------------------------------------------------
    // Mutex serialization: two concurrent invocations must not overlap
    // -------------------------------------------------------------------------

    /**
     * Verifies that the [Mutex] inside [SyncDataUseCase] serializes concurrent callers.
     *
     * Strategy: use a fake [SyncRepository] whose [sync] suspends until a [CompletableDeferred]
     * gate is opened. Two coroutines both call [SyncDataUseCase.invoke] concurrently. We record
     * "enter" and "exit" events and assert that the second sync never starts before the first
     * one finishes (i.e. enter/exit events must not interleave).
     *
     * runTest uses virtual time so no real wall-clock time is consumed.
     */
    @Test
    fun `two concurrent invocations are serialized — second waits for first to finish`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()

        val serializedRepo = object : SyncRepository {
            override suspend fun sync() {
                events += "enter"
                gate.await()
                events += "exit"
            }
            override fun observePendingCount(): Flow<Long> = flowOf(0L)
        }

        val serializedUseCase = SyncDataUseCase(serializedRepo)

        // Launch both coroutines; the second one will block on the mutex until the first exits.
        val job1 = launch { serializedUseCase() }
        val job2 = launch { serializedUseCase() }

        // Advance virtual time so both coroutines start and the first one suspends at gate.await().
        testScheduler.advanceUntilIdle()

        // At this point job1 is suspended at gate.await(); job2 is waiting on the mutex.
        // The active-count of concurrent syncs must never exceed 1.
        val enterCount = events.count { it == "enter" }
        assertEquals(1, enterCount, "Only the first sync should have started before the gate opens")

        // Open the gate so the first sync finishes.
        gate.complete(Unit)
        testScheduler.advanceUntilIdle()

        job1.join()
        job2.join()

        // After both complete: events must be [enter, exit, enter, exit] — strictly serialized.
        assertEquals(listOf("enter", "exit", "enter", "exit"), events)

        // Assert active-count never exceeded 1 (no overlap between enter[i] and exit[i]).
        // The recorded sequence is the proof: the second "enter" appears only after the first "exit".
        val firstExitIndex = events.indexOf("exit")
        val secondEnterIndex = events.lastIndexOf("enter")
        assertTrue(
            firstExitIndex < secondEnterIndex,
            "Second sync must start only after first sync exits",
        )
    }
}
