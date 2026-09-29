package com.emm.justchill.core.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import com.emm.justchill.core.presentation.mvi.MviViewModel
import com.emm.justchill.core.presentation.mvi.UiEffect
import com.emm.justchill.core.presentation.mvi.UiIntent
import com.emm.justchill.core.presentation.mvi.UiState
import kotlinx.coroutines.launch
import org.koin.core.parameter.ParametersDefinition
import org.koin.mp.KoinPlatform
import kotlin.reflect.KClass

class MviHandle<S : UiState, I : UiIntent, E : UiEffect> internal constructor(
    internal val viewModel: MviViewModel<S, I, E>,
    private val store: ViewModelStore,
) {

    val currentState: S get() = viewModel.state.value

    fun send(intent: I) {
        viewModel.onIntent(intent)
    }

    fun collectState(onState: (S) -> Unit) {
        viewModel.viewModelScope.launch { viewModel.state.collect(onState) }
    }

    fun collectEffects(onEffect: (E) -> Unit) {
        viewModel.viewModelScope.launch { viewModel.effect.collect(onEffect) }
    }

    fun clear() {
        store.clear()
    }
}

internal fun <S : UiState, I : UiIntent, E : UiEffect, VM : MviViewModel<S, I, E>> handleOf(
    type: KClass<VM>,
    parameters: ParametersDefinition? = null,
): MviHandle<S, I, E> {
    val store: ViewModelStore = ViewModelStore()
    val factory: ViewModelProvider.Factory = viewModelFactory {
        addInitializer(type) { KoinPlatform.getKoin().get(type, null, parameters) }
    }
    val viewModel: VM = ViewModelProvider.create(store, factory)[type]
    return MviHandle(viewModel, store)
}
