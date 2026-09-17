package com.emm.justchill.core.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

// ADR 009: the backup orchestrator triggers on this edge because backgrounding is when a snapshot
// is cheap — nothing on screen still needs the CPU or the network at that moment.
fun backgroundEvents(): Flow<Unit> = callbackFlow {
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
