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

    // Sign-in only requires non-blank password; existing accounts may have short passwords.
    private val validPassword = "abc"
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

    // ── Shared validateEmail rule coverage ───────────────────────────────────

    @Test
    fun `email with blank local part (at-sign first) throws ValidationError`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("@example.com", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `email with blank domain part (at-sign last) throws ValidationError`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("user@", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `email with multiple at-signs throws ValidationError`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("a@@b", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `minimal email (single char local + at + single char domain) is accepted`() = runTest {
        coEvery { authRepository.signIn("a@b", validPassword) } returns user

        val result = useCase("a@b", validPassword)

        assertEquals(user, result)
        coVerify(exactly = 1) { authRepository.signIn("a@b", validPassword) }
    }

    @Test
    fun `blank password throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validEmail, "   ")
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `short password (under 8 chars) is accepted by sign-in (server is the authority)`() = runTest {
        // Sign-in path only requires non-blank; existing accounts may have short passwords.
        val shortPassword = "abc"
        coEvery { authRepository.signIn(validEmail, shortPassword) } returns user

        val result = useCase(validEmail, shortPassword)

        assertEquals(user, result)
        coVerify(exactly = 1) { authRepository.signIn(validEmail, shortPassword) }
    }

    @Test
    fun `repository failure propagates`() = runTest {
        coEvery { authRepository.signIn(any(), any()) } throws DomainException.Unknown(RuntimeException("network"))

        assertFailsWith<DomainException.Unknown> {
            useCase(validEmail, validPassword)
        }
    }
}
