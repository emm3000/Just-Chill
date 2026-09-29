package com.emm.justchill.core.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

actual fun backgroundEvents(): Flow<Unit> = processLifecycleEvents(Lifecycle.Event.ON_STOP)

actual fun resumeEvents(): Flow<Unit> = processLifecycleEvents(Lifecycle.Event.ON_RESUME)

// flowOn(Main): LifecycleRegistry enforces main-thread addObserver/removeObserver, but the
// orchestrator collects these flows on its Dispatchers.Default application scope.
private fun processLifecycleEvents(edge: Lifecycle.Event): Flow<Unit> = callbackFlow {
    val observer: LifecycleEventObserver = LifecycleEventObserver { _, event ->
        if (event == edge) trySend(Unit)
    }
    val lifecycle: Lifecycle = ProcessLifecycleOwner.get().lifecycle
    lifecycle.addObserver(observer)
    awaitClose { lifecycle.removeObserver(observer) }
}.flowOn(Dispatchers.Main.immediate)
