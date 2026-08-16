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
    private val useCase = SyncDataUseCase(syncRepository, SyncMutex())

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

        val serializedUseCase = SyncDataUseCase(serializedRepo, SyncMutex())

        val job1 = launch { serializedUseCase() }
        val job2 = launch { serializedUseCase() }

        testScheduler.advanceUntilIdle()

        val enterCount = events.count { it == "enter" }
        assertEquals(1, enterCount, "Only the first sync should have started before the gate opens")

        gate.complete(Unit)
        testScheduler.advanceUntilIdle()

        job1.join()
        job2.join()

        assertEquals(listOf("enter", "exit", "enter", "exit"), events)

        val firstExitIndex = events.indexOf("exit")
        val secondEnterIndex = events.lastIndexOf("enter")
        assertTrue(
            firstExitIndex < secondEnterIndex,
            "Second sync must start only after first sync exits",
        )
    }
}
