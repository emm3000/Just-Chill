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
import kotlin.test.assertNull

class SignUpUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val claimLocalDataRepository = mockk<ClaimLocalDataRepository>()
    private val claimLocalDataUseCase = ClaimLocalDataUseCase(claimLocalDataRepository)
    private val useCase = SignUpUseCase(authRepository, claimLocalDataUseCase)

    private val validEmail = "new@example.com"
    private val validPassword = "password1"
    private val user = AuthUser(userId = "uid-2", email = validEmail)

    @Test
    fun `claims local data when AuthUser is returned`() = runTest {
        coEvery { authRepository.signUp(validEmail, validPassword) } returns user
        coEvery { claimLocalDataRepository.claimAll(any()) } just Runs

        val result = useCase(validEmail, validPassword)

        assertEquals(user, result)
        coVerify(exactly = 1) { claimLocalDataRepository.claimAll("uid-2") }
    }

    @Test
    fun `does NOT claim when signUp returns null (email confirmation pending)`() = runTest {
        coEvery { authRepository.signUp(validEmail, validPassword) } returns null

        val result = useCase(validEmail, validPassword)

        assertNull(result)
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `blank email throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("   ", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signUp(any(), any()) }
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `email without at-sign throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase("notanemail", validPassword)
        }
        coVerify(exactly = 0) { authRepository.signUp(any(), any()) }
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `blank password throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validEmail, "     ")
        }
        coVerify(exactly = 0) { authRepository.signUp(any(), any()) }
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `password shorter than 6 chars throws ValidationError and no repository interaction`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validEmail, "ab")
        }
        coVerify(exactly = 0) { authRepository.signUp(any(), any()) }
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `repository failure propagates and claimAll is not called`() = runTest {
        coEvery { authRepository.signUp(any(), any()) } throws DomainException.Unknown(RuntimeException("timeout"))

        assertFailsWith<DomainException.Unknown> {
            useCase(validEmail, validPassword)
        }
        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }
}
