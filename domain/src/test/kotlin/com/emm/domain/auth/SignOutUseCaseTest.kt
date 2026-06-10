package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SignOutUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = SignOutUseCase(repository)

    @Test
    fun `invoke delegates signOut to repository`() = runTest {
        coEvery { repository.signOut() } just Runs

        useCase()

        coVerify(exactly = 1) { repository.signOut() }
    }

    @Test
    fun `invoke propagates exception from repository`() = runTest {
        coEvery { repository.signOut() } throws DomainException.Unknown(RuntimeException("network error"))

        kotlin.test.assertFailsWith<DomainException.Unknown> { useCase() }
    }
}
