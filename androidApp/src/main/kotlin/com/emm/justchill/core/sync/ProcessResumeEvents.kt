package com.emm.justchill.core.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Emits [Unit] on every foreground ON_RESUME event of the process lifecycle.
 *
 * This is the production implementation of the `resumeEvents: Flow<Unit>` parameter on
 * [SyncOrchestrator]. Backed by [ProcessLifecycleOwner] so it fires when the app comes
 * to the foreground (first Activity resumes). [awaitClose] removes the observer when the
 * downstream flow is cancelled.
 */
fun processResumeEvents(): Flow<Unit> = callbackFlow {
    val observer = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            trySend(Unit)
        }
    }
    val lifecycle = ProcessLifecycleOwner.get().lifecycle
    lifecycle.addObserver(observer)
    awaitClose { lifecycle.removeObserver(observer) }
}.flowOn(Dispatchers.Main.immediate)
// flowOn(Main): LifecycleRegistry enforces main-thread addObserver/removeObserver, but the
// orchestrator collects this flow on its Dispatchers.Default application scope.
