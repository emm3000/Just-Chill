package com.emm.justchill.core.lifecycle

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification

/**
 * iOS `actual` of the common [backgroundEvents]. Supplies the iOS background-transition signal
 * closest to Android's ProcessLifecycleOwner `ON_STOP`.
 *
 * Emits [Unit] every time the app leaves the foreground, by observing
 * [UIApplicationDidEnterBackgroundNotification] on the default [NSNotificationCenter].
 *
 * The observer token returned by `addObserverForName` is removed in [awaitClose] to avoid leaking the
 * registration (and the captured `trySend`) once the collector is cancelled.
 */
actual fun backgroundEvents(): Flow<Unit> = callbackFlow {
    val token = NSNotificationCenter.defaultCenter.addObserverForName(
        name = UIApplicationDidEnterBackgroundNotification,
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
