package com.emm.domain.sync

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObservePendingSyncCountUseCaseTest {

    private val syncRepository = mockk<SyncRepository>()
    private val useCase = ObservePendingSyncCountUseCase(syncRepository)

    @Test
    fun `invoke delegates to syncRepository observePendingCount`() = runTest {
        every { syncRepository.observePendingCount() } returns flowOf(5L)

        val result = useCase().toList()

        assertEquals(listOf(5L), result)
        verify(exactly = 1) { syncRepository.observePendingCount() }
    }

    @Test
    fun `invoke returns empty when no pending rows`() = runTest {
        every { syncRepository.observePendingCount() } returns flowOf(0L)

        val result = useCase().toList()

        assertEquals(listOf(0L), result)
    }
}
