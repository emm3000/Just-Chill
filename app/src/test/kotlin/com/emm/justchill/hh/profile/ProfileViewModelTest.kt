package com.emm.justchill.hh.profile

import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.backup.ExportDataUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val exportData = mockk<ExportDataUseCase>()
    private val importData = mockk<ImportDataUseCase>(relaxed = true)
    private val signOut = mockk<SignOutUseCase>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(emptyList())
    }
    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(emptyList())
    }

    // Real controllable flow — replaces the relaxed mock that returns empty flow.
    private val sessionFlow = MutableSharedFlow<SessionStatus>(replay = 1)
    private val observeSession = mockk<ObserveSessionUseCase>(relaxed = true)

    private fun buildViewModel(): ProfileViewModel {
        every { observeSession.invoke() } returns sessionFlow
        return ProfileViewModel(
            exportData = exportData,
            importData = importData,
            signOut = signOut,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            observeSession = observeSession,
        )
    }

    // ── Session state tests ────────────────────────────────────────────────

    @Test
    fun `session emits Authenticated maps to SignedIn with email`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid1", email = "user@example.com")))
        advanceUntilIdle()

        val session = vm.state.value.session
        assertIs<SessionUiState.SignedIn>(session)
        assertEquals("user@example.com", session.email)
    }

    @Test
    fun `session emits Authenticated with null email maps to SignedIn with null email`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid2", email = null)))
        advanceUntilIdle()

        val session = vm.state.value.session
        assertIs<SessionUiState.SignedIn>(session)
        assertEquals(null, session.email)
    }

    @Test
    fun `session emits NotAuthenticated maps to SignedOut`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        sessionFlow.emit(SessionStatus.NotAuthenticated)
        advanceUntilIdle()

        assertEquals(SessionUiState.SignedOut, vm.state.value.session)
    }

    @Test
    fun `SignOut intent invokes SignOutUseCase and emits success message effect`() = runTest(testDispatcher) {
        coEvery { signOut.invoke() } returns Unit

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.SignOut)
        advanceUntilIdle()

        coVerify(exactly = 1) { signOut.invoke() }
        val expectedSignOutText = "Sesión cerrada. Tus datos siguen en este teléfono."
        assertTrue(
            effects.any {
                it is ProfileEffect.ShowMessage && it.text == expectedSignOutText
            },
            "Expected sign-out success message not found in $effects",
        )

        job.cancel()
    }

    @Test
    fun `SignOut failure emits error effect with toUserMessage mapping`() = runTest(testDispatcher) {
        coEvery { signOut.invoke() } throws DomainException.NetworkUnavailable(RuntimeException("no network"))

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.SignOut)
        advanceUntilIdle()

        // NetworkUnavailable.toUserMessage() = "Sin conexión — revisa tu internet"
        assertTrue(
            effects.any { it is ProfileEffect.ShowMessage && it.text == "Sin conexión — revisa tu internet" },
            "Expected network error message not found in $effects",
        )

        job.cancel()
    }

    // ── Export tests (preserved from original) ────────────────────────────

    @Test
    fun `ExportToStream happy path writes json to stream and emits success message`() = runTest(testDispatcher) {
        val expectedJson = """{"version":"1.0","data":[]}"""
        coEvery { exportData(any(), any()) } returns expectedJson

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        val outputStream = ByteArrayOutputStream()
        vm.onIntent(ProfileIntent.ExportToStream(outputStream))
        advanceUntilIdle()

        assertEquals(expectedJson, outputStream.toString(Charsets.UTF_8.name()))
        assertTrue(effects.any { it is ProfileEffect.ShowMessage && it.text == "Listo, tu data está guardada." })
        assertEquals(false, vm.state.value.isExporting)

        job.cancel()
    }

    @Test
    fun `ExportToStream on DatabaseError emits Spanish error message`() = runTest(testDispatcher) {
        val cause = RuntimeException("db failure")
        coEvery { exportData(any(), any()) } throws DomainException.DatabaseError(cause)

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        val outputStream = ByteArrayOutputStream()
        vm.onIntent(ProfileIntent.ExportToStream(outputStream))
        advanceUntilIdle()

        assertTrue(effects.any { it is ProfileEffect.ShowMessage && it.text == "Hubo un problema guardando tu data" })
        assertEquals(false, vm.state.value.isExporting)

        job.cancel()
    }
}
