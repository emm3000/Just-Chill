package com.emm.justchill.core.sync

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification

/**
 * iOS analogue of `:androidApp`'s `processResumeEvents()` (ProcessLifecycleOwner `ON_RESUME`).
 *
 * Emits [Unit] every time the app becomes active in the foreground, by observing
 * [UIApplicationDidBecomeActiveNotification] on the default [NSNotificationCenter]. This is the
 * iOS lifecycle signal closest to Android's `ON_RESUME`: it fires on cold launch becoming active
 * and on every return from background.
 *
 * Wired into [IosSyncOrchestrator] as its `resumeEvents` flow (the platform-injected equivalent of
 * Android's `resumeEvents` constructor parameter), so the on-resume sync trigger reaches Android parity
 * without any commonMain `expect/actual` ceremony — each platform supplies its own resume flow through
 * its own Koin module.
 *
 * The observer token returned by `addObserverForName` is removed in [awaitClose] to avoid leaking the
 * registration (and the captured `trySend`) once the collector is cancelled.
 */
fun iosForegroundEvents(): Flow<Unit> = callbackFlow {
    val token = NSNotificationCenter.defaultCenter.addObserverForName(
        name = UIApplicationDidBecomeActiveNotification,
        `object` = null,
        // null queue → the notification is delivered synchronously on the posting (main) thread.
        // trySend is safe to call from any thread, so no extra dispatch is needed here.
        queue = null as NSOperationQueue?,
    ) { _ ->
        trySend(Unit)
    }

    awaitClose {
        NSNotificationCenter.defaultCenter.removeObserver(token)
    }
}
