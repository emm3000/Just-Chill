package com.emm.justchill.hh.auth

import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.ResendConfirmationEmailUseCase
import com.emm.domain.auth.SignInUseCase
import com.emm.domain.auth.SignInWithGoogleUseCase
import com.emm.domain.auth.SignUpUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val signIn = mockk<SignInUseCase>(relaxed = true)
    private val signUp = mockk<SignUpUseCase>(relaxed = true)
    private val signInWithGoogle = mockk<SignInWithGoogleUseCase>(relaxed = true)
    private val resendConfirmationEmail = mockk<ResendConfirmationEmailUseCase>(relaxed = true)

    private fun buildViewModel(googleClientId: String = "test-client-id") = AuthViewModel(
        signIn = signIn,
        signUp = signUp,
        signInWithGoogle = signInWithGoogle,
        resendConfirmationEmail = resendConfirmationEmail,
        googleServerClientId = googleClientId,
    )

    // ── 1. signUp returns null → CheckEmail step ──────────────────────────

    @Test
    fun `signUp returns null transitions to CheckEmail with trimmed confirmationEmail`() = runTest(testDispatcher) {
        coEvery { signUp.invoke(any(), any()) } returns null

        val vm = buildViewModel()
        vm.onIntent(AuthIntent.EmailChanged("  user@example.com  "))
        vm.onIntent(AuthIntent.PasswordChanged("password123"))
        vm.onIntent(AuthIntent.ToggleMode) // switch to SignUp
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        assertEquals(AuthStep.CheckEmail, vm.state.value.step)
        assertEquals("user@example.com", vm.state.value.confirmationEmail)
        assertFalse(vm.state.value.isLoading, "isLoading must reset after submit")
    }

    @Test
    fun `signUp returns null does not emit NavigateBack`() = runTest(testDispatcher) {
        coEvery { signUp.invoke(any(), any()) } returns null

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.ToggleMode)
        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("pass1234"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        assertFalse(effects.any { it is AuthEffect.NavigateBack }, "NavigateBack must NOT fire on pending confirmation")

        job.cancel()
    }

    // ── 2. signUp returns user → NavigateBack ─────────────────────────────

    @Test
    fun `signUp returns AuthUser emits NavigateBack`() = runTest(testDispatcher) {
        coEvery { signUp.invoke(any(), any()) } returns AuthUser("uid1", "user@example.com")

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.ToggleMode) // SignUp mode
        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("pass1234"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        assertTrue(effects.any { it is AuthEffect.NavigateBack })
        assertEquals(AuthStep.Form, vm.state.value.step)

        job.cancel()
    }

    // ── 3. ResendEmail happy path ─────────────────────────────────────────

    @Test
    fun `ResendEmail calls use case with confirmationEmail and emits ShowMessage`() = runTest(testDispatcher) {
        coEvery { signUp.invoke(any(), any()) } returns null
        coEvery { resendConfirmationEmail.invoke(any()) } returns Unit

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        // Navigate to CheckEmail step first
        vm.onIntent(AuthIntent.ToggleMode)
        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("pass1234"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        vm.onIntent(AuthIntent.ResendEmail)
        advanceUntilIdle()

        coVerify(exactly = 1) { resendConfirmationEmail.invoke("user@example.com") }
        assertTrue(effects.any { it is AuthEffect.ShowMessage && it.message == "Listo, te reenviamos el enlace." })
        assertFalse(vm.state.value.isResending, "isResending must reset to false after success")

        job.cancel()
    }

    // ── 4. ResendEmail error path ─────────────────────────────────────────

    @Test
    fun `ResendEmail failure emits ShowError and resets isResending`() = runTest(testDispatcher) {
        coEvery { signUp.invoke(any(), any()) } returns null
        coEvery { resendConfirmationEmail.invoke(any()) } throws DomainException.NetworkUnavailable(RuntimeException("no net"))

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        // Navigate to CheckEmail step first
        vm.onIntent(AuthIntent.ToggleMode)
        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("pass1234"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        vm.onIntent(AuthIntent.ResendEmail)
        advanceUntilIdle()

        // NetworkUnavailable.toUserMessage() = "Sin conexión — revisa tu internet"
        assertTrue(
            effects.any { it is AuthEffect.ShowError && it.message == "Sin conexión — revisa tu internet" },
            "Expected network error message not found in $effects",
        )
        assertFalse(vm.state.value.isResending, "isResending must reset to false after error")

        job.cancel()
    }

    // ── 5. BackToSignIn ───────────────────────────────────────────────────

    @Test
    fun `BackToSignIn returns to Form step in SignIn mode and clears password`() = runTest(testDispatcher) {
        coEvery { signUp.invoke(any(), any()) } returns null

        val vm = buildViewModel()

        // Navigate to CheckEmail step
        vm.onIntent(AuthIntent.ToggleMode)
        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("secret99"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        assertEquals(AuthStep.CheckEmail, vm.state.value.step)

        vm.onIntent(AuthIntent.BackToSignIn)
        advanceUntilIdle()

        assertEquals(AuthStep.Form, vm.state.value.step)
        assertEquals(AuthMode.SignIn, vm.state.value.mode)
        assertEquals("", vm.state.value.password)
    }

    // ── 6a. Back while CheckEmail → Form (no NavigateBack) ───────────────

    @Test
    fun `Back while CheckEmail transitions to Form without NavigateBack`() = runTest(testDispatcher) {
        coEvery { signUp.invoke(any(), any()) } returns null

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        // Navigate to CheckEmail
        vm.onIntent(AuthIntent.ToggleMode)
        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("pass1234"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        vm.onIntent(AuthIntent.Back)
        advanceUntilIdle()

        assertEquals(AuthStep.Form, vm.state.value.step)
        assertFalse(effects.any { it is AuthEffect.NavigateBack }, "NavigateBack must NOT fire when Back is pressed in CheckEmail")

        job.cancel()
    }

    // ── 6b. Back while Form → NavigateBack ───────────────────────────────

    @Test
    fun `Back while Form emits NavigateBack`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.Back)
        advanceUntilIdle()

        assertTrue(effects.any { it is AuthEffect.NavigateBack })

        job.cancel()
    }

    // ── 7. Sign-in regression ─────────────────────────────────────────────

    @Test
    fun `Submit in SignIn mode with valid credentials emits NavigateBack`() = runTest(testDispatcher) {
        coEvery { signIn.invoke(any(), any()) } returns AuthUser("uid1", "user@example.com")

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("pass1234"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        assertIs<AuthEffect.NavigateBack>(effects.first { it is AuthEffect.NavigateBack })
        assertFalse(vm.state.value.isLoading)

        job.cancel()
    }
}
