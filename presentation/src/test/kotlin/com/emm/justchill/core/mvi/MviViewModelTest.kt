package com.emm.justchill.core.mvi

import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Contract tests for [MviViewModel.launchSafe] — the one funnel every ViewModel routes its
 * suspending work through, so its exception policy is app-wide behaviour.
 *
 * Cancellation is the case that bites in production: `ReportViewModel` keeps a latest-wins job and
 * cancels the in-flight one on every filter change, so a `launchSafe` that mistakes cancellation
 * for a failure turns an ordinary re-query into a spurious error snackbar.
 */
class MviViewModelTest {

    @Before
    fun setUp() {
        // launchSafe runs on viewModelScope (Dispatchers.Main.immediate). Standard rather than
        // Unconfined so the test drives the suspend/cancel ordering explicitly; runTest adopts this
        // dispatcher's scheduler, which is what makes runCurrent/advanceUntilIdle steer the VM.
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cancelling a launchSafe job does not emit an error effect`() = runTest {
        val viewModel = LaunchSafeViewModel()
        val effects: List<TestEffect> = collectEffects(viewModel)

        val job: Job = viewModel.runSafe { awaitCancellation() }
        runCurrent()
        job.cancel()
        settle()

        assertTrue(job.isCancelled, "the job must end cancelled")
        assertEquals(emptyList(), effects, "cancellation is not a failure: no effect may be emitted")
    }

    @Test
    fun `a domain exception in the block reaches onError untouched`() = runTest {
        val viewModel = LaunchSafeViewModel()
        val effects: List<TestEffect> = collectEffects(viewModel)
        val failure = DomainException.NotFound("Transaction")

        viewModel.runSafe { throw failure }
        settle()

        assertSame(failure, effects.single().error, "a DomainException must be handed over as-is")
    }

    @Test
    fun `a non-domain exception reaches onError wrapped as Unknown, preserving the cause`() = runTest {
        val viewModel = LaunchSafeViewModel()
        val effects: List<TestEffect> = collectEffects(viewModel)
        val failure = IllegalStateException("boom")

        viewModel.runSafe { throw failure }
        settle()

        val unknown = assertIs<DomainException.Unknown>(effects.single().error)
        assertSame(failure, unknown.cause, "the original throwable must survive as the cause")
    }

    /**
     * Returns a live view of everything the VM emits. The channel behind `effect` is BUFFERED, so
     * the collector only has to exist before the assertions, not before the emission.
     */
    private fun TestScope.collectEffects(viewModel: LaunchSafeViewModel): List<TestEffect> {
        val effects = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effect.collect { effects += it } }
        runCurrent()
        return effects
    }

    /**
     * Drains everything the ViewModel queued, the effect collector included.
     *
     * `advanceUntilIdle` stops as soon as no FOREGROUND task is left, and the collector above runs
     * in `backgroundScope`. On its own it therefore returns before the collector appends, and every
     * "no effect was emitted" assertion here would pass vacuously. `runCurrent` has no such filter.
     */
    private fun TestScope.settle() {
        advanceUntilIdle()
        runCurrent()
    }
}

private object TestState : UiState

private object TestIntent : UiIntent

private class TestEffect(val error: DomainException) : UiEffect

private class LaunchSafeViewModel : MviViewModel<TestState, TestIntent, TestEffect>(TestState) {

    override fun onIntent(intent: TestIntent) = Unit

    /** `launchSafe` is protected; only a subclass can hand its [Job] to the test. */
    fun runSafe(block: suspend () -> Unit): Job = launchSafe(onError = ::TestEffect, block = block)
}
