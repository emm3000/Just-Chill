package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class ResendConfirmationEmailUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val useCase = ResendConfirmationEmailUseCase(authRepository)

    private val validEmail = "user@example.com"

    @Test
    fun `happy path delegates to repository`() = runTest {
        coEvery { authRepository.resendConfirmationEmail(validEmail) } returns Unit

        useCase(validEmail)

        coVerify(exactly = 1) { authRepository.resendConfirmationEmail(validEmail) }
    }

    @Test
    fun `blank email throws ValidationError without calling repository`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("   ")
        }
        coVerify(exactly = 0) { authRepository.resendConfirmationEmail(any()) }
    }

    @Test
    fun `empty email throws ValidationError without calling repository`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("")
        }
        coVerify(exactly = 0) { authRepository.resendConfirmationEmail(any()) }
    }

    @Test
    fun `email without at-sign throws ValidationError without calling repository`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("notanemail")
        }
        coVerify(exactly = 0) { authRepository.resendConfirmationEmail(any()) }
    }

    @Test
    fun `repository failure propagates`() = runTest {
        coEvery { authRepository.resendConfirmationEmail(validEmail) } throws
            DomainException.NetworkUnavailable(RuntimeException("timeout"))

        assertFailsWith<DomainException.NetworkUnavailable> {
            useCase(validEmail)
        }
    }
}
