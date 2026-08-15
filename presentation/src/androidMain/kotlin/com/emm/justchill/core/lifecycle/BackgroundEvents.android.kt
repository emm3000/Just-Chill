package com.emm.justchill.core.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Android `actual` of the common [backgroundEvents]. Emits [Unit] on every `ON_STOP` event of the
 * process lifecycle, backed by [ProcessLifecycleOwner] so it fires when the app leaves the
 * foreground (last Activity stops). [awaitClose] removes the observer when the downstream flow is
 * cancelled.
 */
actual fun backgroundEvents(): Flow<Unit> = callbackFlow {
    val observer = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            trySend(Unit)
        }
    }
    val lifecycle = ProcessLifecycleOwner.get().lifecycle
    lifecycle.addObserver(observer)
    awaitClose { lifecycle.removeObserver(observer) }
}.flowOn(Dispatchers.Main.immediate)
// flowOn(Main): LifecycleRegistry enforces main-thread addObserver/removeObserver, but the
// orchestrator collects this flow on its Dispatchers.Default application scope.
