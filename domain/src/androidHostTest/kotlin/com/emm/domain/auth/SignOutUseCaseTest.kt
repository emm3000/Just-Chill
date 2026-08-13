package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class SignOutUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = SignOutUseCase(repository)

    @Test
    fun `invoke delegates signOut to repository and returns Revoked unchanged`() = runTest {
        coEvery { repository.signOut() } returns SignOutResult.Revoked

        val result = useCase()

        assertEquals(SignOutResult.Revoked, result)
        coVerify(exactly = 1) { repository.signOut() }
    }

    @Test
    fun `invoke propagates LocalOnly unchanged`() = runTest {
        coEvery { repository.signOut() } returns SignOutResult.LocalOnly

        val result = useCase()

        assertEquals(SignOutResult.LocalOnly, result)
    }

    @Test
    fun `invoke propagates exception from repository`() = runTest {
        coEvery { repository.signOut() } throws DomainException.Unknown(RuntimeException("network error"))

        kotlin.test.assertFailsWith<DomainException.Unknown> { useCase() }
    }
}
