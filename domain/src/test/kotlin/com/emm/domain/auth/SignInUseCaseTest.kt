package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SignInUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val claimLocalDataRepository = mockk<ClaimLocalDataRepository>()
    private val claimLocalDataUseCase = ClaimLocalDataUseCase(claimLocalDataRepository)
    private val useCase = SignInUseCase(authRepository, claimLocalDataUseCase)

    private val validEmail = "user@example.com"
    private val validPassword = "secret"
    private val user = AuthUser(userId = "uid-1", email = validEmail)

    @Test
    fun `happy path returns AuthUser and claims with the signed-in userId`() = runTest {
        coEvery { authRepository.signIn(validEmail, validPassword) } returns user
        coEvery { claimLocalDataRepository.claimAll(any()) } just Runs

        val result = useCase(validEmail, validPassword)

        assertEquals(user, result)
        coVerify(exactly = 1) { authRepository.signIn(validEmail, validPassword) }
        coVerify(exactly = 1) { claimLocalDataRepository.claimAll("uid-1") }
    }

    @Test
    fun `signIn is called before claimAll`() = runTest {
        val callOrder = mutableListOf<String>()
        coEvery { authRepository.signIn(any(), any()) } answers {
            callOrder += "signIn"
            user
        }
        coEvery { claimLocalDataRepository.claimAll(any()) } answers { callOrder += "claimAll" }

        useCase(validEmail, validPassword)

        assert(callOrder.indexOf("signIn") < callOrder.indexOf("claimAll")) {
            "Expected signIn before claimAll but got: $callOrder"
        }
    }

    @Test
    fun `blank email throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("   ", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `empty email throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `email without at-sign throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("notanemail", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `blank password throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validEmail, "   ")
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `password shorter than 6 chars throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validEmail, "abc")
        }
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `repository failure propagates and claimAll is not called`() = runTest {
        coEvery { authRepository.signIn(any(), any()) } throws DomainException.Unknown(RuntimeException("network"))

        assertFailsWith<DomainException.Unknown> {
            useCase(validEmail, validPassword)
        }
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }
}
