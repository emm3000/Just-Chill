package com.emm.justchill.core.ui.mvi

import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.testing.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

// launchSafe and launchSafeIn are the one funnel every ViewModel routes its suspending work and
// collectors through, so their exception policy is app-wide behaviour and is pinned here so a
// future split cannot let the two drift.
class MviViewModelTest {

    // Standard rather than Unconfined so the test drives the suspend/cancel ordering explicitly;
    // runTest adopts this dispatcher's scheduler, which is what makes runCurrent/advanceUntilIdle
    // steer the VM.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `cancelling a launchSafe job does not emit an error effect`() = runTest {
        val viewModel = FunnelViewModel()
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
        val viewModel = FunnelViewModel()
        val effects: List<TestEffect> = collectEffects(viewModel)
        val failure = DomainException.NotFound("Transaction")

        viewModel.runSafe { throw failure }
        settle()

        assertSame(failure, effects.single().error, "a DomainException must be handed over as-is")
    }

    @Test
    fun `a non-domain exception reaches onError wrapped as Unknown, preserving the cause`() = runTest {
        val viewModel = FunnelViewModel()
        val effects: List<TestEffect> = collectEffects(viewModel)
        val failure = IllegalStateException("boom")

        viewModel.runSafe { throw failure }
        settle()

        val unknown = assertIs<DomainException.Unknown>(effects.single().error)
        assertSame(failure, unknown.cause, "the original throwable must survive as the cause")
    }

    @Test
    fun `cancelling a launchSafeIn collector does not emit an error effect`() = runTest {
        val viewModel = FunnelViewModel()
        val effects: List<TestEffect> = collectEffects(viewModel)

        val job: Job = viewModel.collectSafe(flow { awaitCancellation() })
        runCurrent()
        job.cancel()
        settle()

        assertTrue(job.isCancelled, "the collector must end cancelled")
        assertEquals(emptyList(), effects, "cancellation is not a failure: no effect may be emitted")
    }

    @Test
    fun `a domain exception in the collected flow reaches onError untouched`() = runTest {
        val viewModel = FunnelViewModel()
        val effects: List<TestEffect> = collectEffects(viewModel)
        val failure = DomainException.NotFound("Loan")

        viewModel.collectSafe(flow { throw failure })
        settle()

        assertSame(failure, effects.single().error, "a DomainException must be handed over as-is")
    }

    @Test
    fun `a collector that keeps failing is subscribed four times and reports once`() = runTest {
        val viewModel = FunnelViewModel()
        val effects: List<TestEffect> = collectEffects(viewModel)
        var subscriptions = 0

        viewModel.collectSafe(
            flow {
                subscriptions++
                throw DomainException.DatabaseError(RuntimeException("database is locked"))
            },
        )
        settle()

        assertEquals(4, subscriptions, "the policy is one attempt plus three retries")
        assertEquals(1, effects.size, "a failure episode is one snackbar, however many attempts it took")
    }

    @Test
    fun `a collector that fails once recovers on its retry, silently`() = runTest {
        val viewModel = FunnelViewModel()
        val effects: List<TestEffect> = collectEffects(viewModel)
        var subscriptions = 0

        val job: Job = viewModel.collectSafe(
            flow {
                subscriptions++
                if (subscriptions == 1) throw DomainException.DatabaseError(RuntimeException("database is locked"))
                emit(Unit)
            },
        )
        settle()

        assertEquals(2, subscriptions, "the retry must re-subscribe, and stop once it succeeds")
        assertEquals(emptyList(), effects, "a failure the retry absorbs never reaches the user")
        assertTrue(job.isCompleted, "the collector must end with the flow it recovered on")
    }

    @Test
    fun `a CancellationException raised inside the collected flow is neither retried nor reported`() = runTest {
        val viewModel = FunnelViewModel()
        val effects: List<TestEffect> = collectEffects(viewModel)
        var subscriptions = 0

        val job: Job = viewModel.collectSafe(
            flow {
                subscriptions++
                throw CancellationException("upstream gave up")
            },
        )
        settle()

        assertEquals(1, subscriptions, "cancellation is not a retryable failure")
        assertEquals(emptyList(), effects, "cancellation is not a failure: no effect may be emitted")
        assertTrue(job.isCancelled, "the collector must end cancelled")
    }

    @Test
    fun `a non-domain exception in the collected flow reaches onError wrapped as Unknown`() = runTest {
        val viewModel = FunnelViewModel()
        val effects: List<TestEffect> = collectEffects(viewModel)
        val failure = IllegalStateException("boom")

        viewModel.collectSafe(flow { throw failure })
        settle()

        val unknown = assertIs<DomainException.Unknown>(effects.single().error)
        assertSame(failure, unknown.cause, "the original throwable must survive as the cause")
    }

    // The channel behind effect is BUFFERED, so the collector only has to exist before the
    // assertions, not before the emission.
    private fun TestScope.collectEffects(viewModel: FunnelViewModel): List<TestEffect> {
        val effects = mutableListOf<TestEffect>()
        backgroundScope.launch { viewModel.effect.collect { effects += it } }
        runCurrent()
        return effects
    }

    // advanceUntilIdle stops as soon as no FOREGROUND task is left, and collectEffects' collector
    // runs in backgroundScope; runCurrent has no such filter and is what actually drains it.
    private fun TestScope.settle() {
        advanceUntilIdle()
        runCurrent()
    }
}

private object TestState : UiState

private object TestIntent : UiIntent

private class TestEffect(val error: DomainException) : UiEffect

private class FunnelViewModel : MviViewModel<TestState, TestIntent, TestEffect>(TestState) {

    override fun onIntent(intent: TestIntent) = Unit

    // launchSafe/launchSafeIn are protected; only a subclass can hand their Job to the test.
    fun runSafe(block: suspend () -> Unit): Job = launchSafe(onError = ::TestEffect, block = block)

    fun collectSafe(source: Flow<Unit>): Job = source.launchSafeIn(onError = ::TestEffect)
}
