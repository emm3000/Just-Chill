package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SignInUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val useCase = SignInUseCase(authRepository)

    private val validEmail = "user@example.com"
    private val validPassword = "secret"
    private val user = AuthUser(userId = "uid-1", email = validEmail)

    @Test
    fun `happy path returns AuthUser`() = runTest {
        coEvery { authRepository.signIn(validEmail, validPassword) } returns user

        val result = useCase(validEmail, validPassword)

        assertEquals(user, result)
        coVerify(exactly = 1) { authRepository.signIn(validEmail, validPassword) }
    }

    @Test
    fun `blank email throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("   ", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `empty email throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `email without at-sign throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("notanemail", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `blank password throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validEmail, "   ")
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `password shorter than 6 chars throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validEmail, "abc")
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `repository failure propagates`() = runTest {
        coEvery { authRepository.signIn(any(), any()) } throws DomainException.Unknown(RuntimeException("network"))

        assertFailsWith<DomainException.Unknown> {
            useCase(validEmail, validPassword)
        }
    }
}
