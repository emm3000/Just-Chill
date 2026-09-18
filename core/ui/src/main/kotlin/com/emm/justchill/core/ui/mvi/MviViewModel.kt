package com.emm.justchill.core.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.domain.shared.error.DomainException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val COLLECTOR_RETRIES = 3L
private const val COLLECTOR_RETRY_BASE_DELAY_MS = 200L

abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(protected val initialState: S) : ViewModel() {

    private val _state: MutableStateFlow<S> = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect: Channel<E> = Channel(Channel.BUFFERED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    protected val currentState: S get() = _state.value

    protected fun updateState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effect.send(effect) }
    }

    protected fun launchSafe(onError: (DomainException) -> E, block: suspend () -> Unit): Job =
        viewModelScope.launch { funnel(onError, block) }

    protected fun <T> Flow<T>.launchSafeIn(onError: (DomainException) -> E): Job =
        viewModelScope.launch { funnel(onError) { retryOnFailure().collect() } }

    // A collector that dies is dead for the ViewModel's life: funnel catches outside collect(), so
    // a bottom-bar tab never gets a fresh one. This heals SQLite lock contention, gone long before a
    // user could act on a snackbar, not a persistent failure.
    private fun <T> Flow<T>.retryOnFailure(): Flow<T> = retryWhen { cause, attempt ->
        // retryWhen rethrows only the collecting job's own cancellation cause; any other one reaches
        // this predicate and would otherwise count as retryable.
        val retryable = cause !is CancellationException && attempt < COLLECTOR_RETRIES
        if (retryable) delay(COLLECTOR_RETRY_BASE_DELAY_MS shl attempt.toInt())
        retryable
    }

    // Intentional broad catch: CancellationException is rethrown first — it is an Exception, so the
    // broad catch below would otherwise turn every cancelled job into a spurious error effect.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun funnel(onError: (DomainException) -> E, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: DomainException) {
            sendEffect(onError(e))
        } catch (e: Exception) {
            sendEffect(onError(DomainException.Unknown(e)))
        }
    }

    abstract fun onIntent(intent: I)
}
