package com.emm.domain.shared.backup

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ExportDataUseCaseTest {

    private val backupRepository = mockk<BackupRepository>()
    private val useCase = ExportDataUseCase(backupRepository)

    @Test
    fun `delegates to backupRepository with the received exportedAt and appVersion`() = runTest {
        val exportedAt = 1_748_000_000_000L
        val appVersion = "1.0.0"
        coEvery { backupRepository.exportToJson(exportedAt, appVersion) } returns "{}"

        useCase(exportedAt, appVersion)

        coVerify(exactly = 1) { backupRepository.exportToJson(exportedAt, appVersion) }
    }

    @Test
    fun `returns exactly what backupRepository returns`() = runTest {
        val expected = """{"schemaVersion":1}"""
        coEvery { backupRepository.exportToJson(any(), any()) } returns expected

        val result = useCase(exportedAt = 0L, appVersion = "0.0.1")

        assertEquals(expected, result)
    }
}
