package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ClaimLocalDataUseCaseTest {

    private val repository = mockk<ClaimLocalDataRepository>()
    private val useCase = ClaimLocalDataUseCase(repository)

    @Test
    fun `invoke delegates claimAll with the given userId`() = runTest {
        coEvery { repository.claimAll(any()) } just Runs

        useCase("user-123")

        coVerify(exactly = 1) { repository.claimAll("user-123") }
    }

    @Test
    fun `invoke propagates exception from repository`() = runTest {
        coEvery { repository.claimAll(any()) } throws DomainException.Unknown(RuntimeException("db error"))

        kotlin.test.assertFailsWith<DomainException.Unknown> { useCase("user-abc") }
    }
}
