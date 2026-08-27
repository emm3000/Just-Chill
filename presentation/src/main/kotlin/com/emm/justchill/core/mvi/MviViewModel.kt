package com.emm.justchill.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    // Intentional broad catch: launchSafe is the VM-level adapter that funnels every
    // non-domain throwable into DomainException.Unknown(cause = e), preserving the original.
    // CancellationException is rethrown first — it is an Exception, so the broad catch below would
    // otherwise turn every cancelled job into a spurious error effect (sendEffect launches on the
    // still-alive viewModelScope, so the snackbar really does reach the user).
    @Suppress("TooGenericExceptionCaught")
    protected fun launchSafe(onError: (DomainException) -> E, block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            // Must not be swallowed — propagate to the coroutine machinery.
            throw e
        } catch (e: DomainException) {
            sendEffect(onError(e))
        } catch (e: Exception) {
            sendEffect(onError(DomainException.Unknown(e)))
        }
    }

    abstract fun onIntent(intent: I)
}
