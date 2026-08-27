package com.emm.domain.shared.backup

import com.emm.domain.shared.error.DomainException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImportDataUseCaseTest {

    private val backupRepository = mockk<BackupRepository>()
    private val useCase = ImportDataUseCase(backupRepository)

    @Test
    fun `delegates to backupRepository and returns the stats it provides`() = runTest {
        val expected = ImportStats(
            accounts = 2,
            categories = 5,
            transactions = 42,
            recurring = 7,
            loans = 3,
            loanPayments = 11,
        )
        coEvery { backupRepository.importFromJson(any()) } returns expected

        val result = useCase("{}")

        coVerify(exactly = 1) { backupRepository.importFromJson("{}") }
        assertEquals(expected, result)
    }

    @Test
    fun `propagates ValidationError thrown by backupRepository`() = runTest {
        coEvery { backupRepository.importFromJson(any()) } throws
            DomainException.ValidationError("Archivo no válido o corrupto")

        assertFailsWith<DomainException.ValidationError> {
            useCase("{ not valid json }")
        }
    }
}
