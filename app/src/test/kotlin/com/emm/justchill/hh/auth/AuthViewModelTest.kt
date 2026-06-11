package com.emm.justchill.hh.auth

import android.util.Log
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.ResendConfirmationEmailUseCase
import com.emm.domain.auth.SignInUseCase
import com.emm.domain.auth.SignInWithGoogleUseCase
import com.emm.domain.auth.SignUpUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
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
    private val googleSignInLauncher = mockk<GoogleSignInLauncher>(relaxed = true)

    @Before
    fun setup() {
        // android.util.Log is not available in JVM unit tests; stub it out.
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    private fun buildViewModel(googleClientId: String = "test-client-id") = AuthViewModel(
        signIn = signIn,
        signUp = signUp,
        signInWithGoogle = signInWithGoogle,
        resendConfirmationEmail = resendConfirmationEmail,
        googleServerClientId = googleClientId,
        googleSignInLauncher = googleSignInLauncher,
    )

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun TestScope.navigateToCheckEmail(vm: AuthViewModel) {
        coEvery { signUp.invoke(any(), any()) } returns com.emm.domain.auth.SignUpResult.ConfirmationPending
        vm.onIntent(AuthIntent.ToggleMode) // switch to SignUp
        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("pass1234"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()
    }

    // ── 1. Sign-in happy path ────────────────────────────────────────────────

    @Test
    fun `sign-in submit happy path emits NavigateBack`() = runTest(testDispatcher) {
        coEvery { signIn.invoke(any(), any()) } returns AuthUser("uid1", "user@example.com")

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("pass1234"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        assertTrue(effects.any { it is AuthEffect.NavigateBack })
        val formState = assertIs<AuthUiState.Form>(vm.state.value)
        assertFalse(formState.isSubmitting)

        job.cancel()
    }

    @Test
    fun `sign-in submit error emits ShowError with DomainException`() = runTest(testDispatcher) {
        val error = DomainException.Unauthorized("Bad credentials")
        coEvery { signIn.invoke(any(), any()) } throws error

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("pass1234"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        val showError = effects.filterIsInstance<AuthEffect.ShowError>().firstOrNull()
        assertIs<AuthEffect.ShowError>(showError)
        assertEquals(error, showError.error)
        val formState = assertIs<AuthUiState.Form>(vm.state.value)
        assertFalse(formState.isSubmitting)

        job.cancel()
    }

    // ── 2. Sign-up paths ─────────────────────────────────────────────────────

    @Test
    fun `signUp ConfirmationPending transitions to CheckEmail with trimmed email`() = runTest(testDispatcher) {
        coEvery { signUp.invoke(any(), any()) } returns com.emm.domain.auth.SignUpResult.ConfirmationPending

        val vm = buildViewModel()
        vm.onIntent(AuthIntent.EmailChanged("  user@example.com  "))
        vm.onIntent(AuthIntent.PasswordChanged("password123"))
        vm.onIntent(AuthIntent.ToggleMode)
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        val checkEmailState = assertIs<AuthUiState.CheckEmail>(vm.state.value)
        assertEquals("user@example.com", checkEmailState.email)
    }

    @Test
    fun `signUp ConfirmationPending does not emit NavigateBack`() = runTest(testDispatcher) {
        coEvery { signUp.invoke(any(), any()) } returns com.emm.domain.auth.SignUpResult.ConfirmationPending

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.ToggleMode)
        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("pass1234"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        assertFalse(effects.any { it is AuthEffect.NavigateBack })

        job.cancel()
    }

    @Test
    fun `signUp SignedIn emits NavigateBack`() = runTest(testDispatcher) {
        coEvery { signUp.invoke(any(), any()) } returns
            com.emm.domain.auth.SignUpResult.SignedIn(AuthUser("uid1", "user@example.com"))

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.ToggleMode)
        vm.onIntent(AuthIntent.EmailChanged("user@example.com"))
        vm.onIntent(AuthIntent.PasswordChanged("pass1234"))
        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        assertTrue(effects.any { it is AuthEffect.NavigateBack })
        assertIs<AuthUiState.Form>(vm.state.value)

        job.cancel()
    }

    // ── 3. Intents in wrong state are no-ops ─────────────────────────────────

    @Test
    fun `Submit while in CheckEmail is a no-op`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        navigateToCheckEmail(vm)

        assertIs<AuthUiState.CheckEmail>(vm.state.value)

        // Reset mocks so we can detect if signUp/signIn were called
        coEvery { signIn.invoke(any(), any()) } returns AuthUser("uid1", "u@e.com")
        coEvery { signUp.invoke(any(), any()) } returns com.emm.domain.auth.SignUpResult.ConfirmationPending

        vm.onIntent(AuthIntent.Submit)
        advanceUntilIdle()

        // Still in CheckEmail — no state transition
        assertIs<AuthUiState.CheckEmail>(vm.state.value)
        coVerify(exactly = 0) { signIn.invoke(any(), any()) }
        // signUp was called once during navigateToCheckEmail, not again
        coVerify(exactly = 1) { signUp.invoke(any(), any()) }
    }

    @Test
    fun `ResendEmail while in Form is a no-op`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        assertIs<AuthUiState.Form>(vm.state.value)

        vm.onIntent(AuthIntent.ResendEmail)
        advanceUntilIdle()

        coVerify(exactly = 0) { resendConfirmationEmail.invoke(any()) }
        assertIs<AuthUiState.Form>(vm.state.value)
    }

    // ── 4. Back intent ───────────────────────────────────────────────────────

    @Test
    fun `Back while CheckEmail transitions to fresh Form with SignIn mode and empty fields`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        navigateToCheckEmail(vm)

        assertIs<AuthUiState.CheckEmail>(vm.state.value)

        vm.onIntent(AuthIntent.Back)
        advanceUntilIdle()

        val formState = assertIs<AuthUiState.Form>(vm.state.value)
        assertEquals(AuthMode.SignIn, formState.mode)
        assertEquals("", formState.email)
        assertEquals("", formState.password)
    }

    @Test
    fun `Back while CheckEmail does not emit NavigateBack`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        navigateToCheckEmail(vm)
        vm.onIntent(AuthIntent.Back)
        advanceUntilIdle()

        assertFalse(effects.any { it is AuthEffect.NavigateBack })

        job.cancel()
    }

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

    @Test
    fun `BackToSignIn returns fresh Form with SignIn mode and empty fields`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        navigateToCheckEmail(vm)

        vm.onIntent(AuthIntent.BackToSignIn)
        advanceUntilIdle()

        val formState = assertIs<AuthUiState.Form>(vm.state.value)
        assertEquals(AuthMode.SignIn, formState.mode)
        assertEquals("", formState.email)
        assertEquals("", formState.password)
    }

    // ── 5. Google flow via fake launcher ─────────────────────────────────────

    @Test
    fun `Google Success calls signInWithGoogle and emits NavigateBack, isSubmitting false at end`() = runTest(testDispatcher) {
        coEvery { googleSignInLauncher.signIn(any()) } returns
            GoogleCredentialClient.Result.Success(idToken = "token", rawNonce = "nonce")
        coEvery { signInWithGoogle.invoke(any(), any()) } returns AuthUser("uid1", "g@g.com")

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.GoogleSignInClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { signInWithGoogle.invoke("token", "nonce") }
        assertTrue(effects.any { it is AuthEffect.NavigateBack })
        val formState = assertIs<AuthUiState.Form>(vm.state.value)
        assertFalse(formState.isSubmitting)

        job.cancel()
    }

    @Test
    fun `Google Cancelled produces no effect and isSubmitting is false`() = runTest(testDispatcher) {
        coEvery { googleSignInLauncher.signIn(any()) } returns GoogleCredentialClient.Result.Cancelled

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.GoogleSignInClicked)
        advanceUntilIdle()

        assertTrue(effects.isEmpty())
        val formState = assertIs<AuthUiState.Form>(vm.state.value)
        assertFalse(formState.isSubmitting)

        job.cancel()
    }

    @Test
    fun `Google NoCredentials emits Notify GoogleAccountUnavailable`() = runTest(testDispatcher) {
        coEvery { googleSignInLauncher.signIn(any()) } returns GoogleCredentialClient.Result.NoCredentials

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.GoogleSignInClicked)
        advanceUntilIdle()

        val notify = effects.filterIsInstance<AuthEffect.Notify>().firstOrNull()
        assertIs<AuthEffect.Notify>(notify)
        assertEquals(AuthMessage.GoogleAccountUnavailable, notify.message)

        job.cancel()
    }

    @Test
    fun `Google Failure emits Notify GoogleSignInFailed`() = runTest(testDispatcher) {
        coEvery { googleSignInLauncher.signIn(any()) } returns
            GoogleCredentialClient.Result.Failure(RuntimeException("crash"))

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.GoogleSignInClicked)
        advanceUntilIdle()

        val notify = effects.filterIsInstance<AuthEffect.Notify>().firstOrNull()
        assertIs<AuthEffect.Notify>(notify)
        assertEquals(AuthMessage.GoogleSignInFailed, notify.message)

        job.cancel()
    }

    @Test
    fun `blank serverClientId emits Notify GoogleSignInFailed without calling launcher`() = runTest(testDispatcher) {
        val vm = buildViewModel(googleClientId = "")
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AuthIntent.GoogleSignInClicked)
        advanceUntilIdle()

        val notify = effects.filterIsInstance<AuthEffect.Notify>().firstOrNull()
        assertIs<AuthEffect.Notify>(notify)
        assertEquals(AuthMessage.GoogleSignInFailed, notify.message)
        coVerify(exactly = 0) { googleSignInLauncher.signIn(any()) }

        job.cancel()
    }

    @Test
    fun `GoogleSignInClicked while already submitting calls launcher exactly once`() = runTest(testDispatcher) {
        // Make the first signIn call suspend until we advance
        coEvery { googleSignInLauncher.signIn(any()) } returns GoogleCredentialClient.Result.Cancelled

        val vm = buildViewModel()

        vm.onIntent(AuthIntent.GoogleSignInClicked)
        // Second click before advancing — should be no-op because isSubmitting = true
        vm.onIntent(AuthIntent.GoogleSignInClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { googleSignInLauncher.signIn(any()) }
    }

    // ── 6. ResendEmail ───────────────────────────────────────────────────────

    @Test
    fun `ResendEmail happy path emits Notify ConfirmationLinkResent and resets isResending`() = runTest(testDispatcher) {
        coEvery { resendConfirmationEmail.invoke(any()) } returns Unit

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        navigateToCheckEmail(vm)
        vm.onIntent(AuthIntent.ResendEmail)
        advanceUntilIdle()

        coVerify(exactly = 1) { resendConfirmationEmail.invoke("user@example.com") }
        val notify = effects.filterIsInstance<AuthEffect.Notify>().firstOrNull()
        assertIs<AuthEffect.Notify>(notify)
        assertEquals(AuthMessage.ConfirmationLinkResent, notify.message)
        val checkState = assertIs<AuthUiState.CheckEmail>(vm.state.value)
        assertFalse(checkState.isResending)

        job.cancel()
    }

    @Test
    fun `ResendEmail error emits ShowError and resets isResending`() = runTest(testDispatcher) {
        val error = DomainException.NetworkUnavailable(RuntimeException("no net"))
        coEvery { resendConfirmationEmail.invoke(any()) } throws error

        val vm = buildViewModel()
        val effects = mutableListOf<AuthEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        navigateToCheckEmail(vm)
        vm.onIntent(AuthIntent.ResendEmail)
        advanceUntilIdle()

        val showError = effects.filterIsInstance<AuthEffect.ShowError>().firstOrNull()
        assertIs<AuthEffect.ShowError>(showError)
        assertEquals(error, showError.error)
        val checkState = assertIs<AuthUiState.CheckEmail>(vm.state.value)
        assertFalse(checkState.isResending)

        job.cancel()
    }

    @Test
    fun `ResendEmail sets isResending true during call then resets`() = runTest(testDispatcher) {
        coEvery { resendConfirmationEmail.invoke(any()) } returns Unit

        val vm = buildViewModel()
        navigateToCheckEmail(vm)

        vm.onIntent(AuthIntent.ResendEmail)
        // Before advancing, isResending should be true
        val checkStateDuring = vm.state.value as? AuthUiState.CheckEmail
        assertTrue(checkStateDuring?.isResending == true)

        advanceUntilIdle()

        val checkStateAfter = assertIs<AuthUiState.CheckEmail>(vm.state.value)
        assertFalse(checkStateAfter.isResending)
    }
}
