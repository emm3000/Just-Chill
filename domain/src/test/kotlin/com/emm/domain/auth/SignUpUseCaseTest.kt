package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SignUpUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val useCase = SignUpUseCase(authRepository)

    private val validEmail = "new@example.com"
    // Sign-up requires at least 8 characters.
    private val validPassword = "password1" // 9 chars — well above the 8-char minimum
    private val user = AuthUser(userId = "uid-2", email = validEmail)

    @Test
    fun `returns AuthUser when a session was established`() = runTest {
        coEvery { authRepository.signUp(validEmail, validPassword) } returns user

        val result = useCase(validEmail, validPassword)

        assertEquals(user, result)
        coVerify(exactly = 1) { authRepository.signUp(validEmail, validPassword) }
    }

    @Test
    fun `returns null when signUp returns null (email confirmation pending)`() = runTest {
        coEvery { authRepository.signUp(validEmail, validPassword) } returns null

        val result = useCase(validEmail, validPassword)

        assertNull(result)
    }

    @Test
    fun `blank email throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("   ", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signUp(any(), any()) }
    }

    @Test
    fun `email without at-sign throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("notanemail", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signUp(any(), any()) }
    }

    @Test
    fun `blank password throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validEmail, "     ")
        }
        coVerify(exactly = 0) { authRepository.signUp(any(), any()) }
    }

    @Test
    fun `password with 7 chars throws ValidationError (minimum is 8)`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validEmail, "1234567") // exactly 7 chars
        }
        coVerify(exactly = 0) { authRepository.signUp(any(), any()) }
    }

    @Test
    fun `password with exactly 8 chars is accepted`() = runTest {
        val eightCharPassword = "12345678"
        coEvery { authRepository.signUp(validEmail, eightCharPassword) } returns user

        val result = useCase(validEmail, eightCharPassword)

        assertEquals(user, result)
        coVerify(exactly = 1) { authRepository.signUp(validEmail, eightCharPassword) }
    }

    @Test
    fun `repository failure propagates`() = runTest {
        coEvery { authRepository.signUp(any(), any()) } throws DomainException.Unknown(RuntimeException("timeout"))

        assertFailsWith<DomainException.Unknown> {
            useCase(validEmail, validPassword)
        }
    }
}
