package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SignInWithGoogleUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val useCase = SignInWithGoogleUseCase(authRepository)

    private val validToken = "eyJhbGciOiJSUzI1NiJ9.token"
    private val rawNonce = "random-nonce-uuid"
    private val user = AuthUser(userId = "uid-google-1", email = "user@gmail.com")

    @Test
    fun `happy path returns AuthUser`() = runTest {
        coEvery { authRepository.signInWithGoogle(validToken, rawNonce) } returns user

        val result = useCase(validToken, rawNonce)

        assertEquals(user, result)
        coVerify(exactly = 1) { authRepository.signInWithGoogle(validToken, rawNonce) }
    }

    @Test
    fun `blank nonce throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validToken, "   ")
        }
        coVerify(exactly = 0) { authRepository.signInWithGoogle(any(), any()) }
    }

    @Test
    fun `blank token throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("   ", rawNonce)
        }
        coVerify(exactly = 0) { authRepository.signInWithGoogle(any(), any()) }
    }

    @Test
    fun `empty token throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("", rawNonce)
        }
        coVerify(exactly = 0) { authRepository.signInWithGoogle(any(), any()) }
    }

    @Test
    fun `repository failure propagates`() = runTest {
        coEvery {
            authRepository.signInWithGoogle(any(), any())
        } throws DomainException.Unauthorized("Invalid Google ID token")

        assertFailsWith<DomainException.Unauthorized> {
            useCase(validToken, rawNonce)
        }
    }
}
