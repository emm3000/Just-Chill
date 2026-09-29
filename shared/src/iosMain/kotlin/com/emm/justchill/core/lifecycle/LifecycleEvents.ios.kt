package com.emm.justchill.core.lifecycle

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotificationName
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.darwin.NSObjectProtocol

actual fun backgroundEvents(): Flow<Unit> = applicationNotifications(UIApplicationDidEnterBackgroundNotification)

actual fun resumeEvents(): Flow<Unit> = applicationNotifications(UIApplicationDidBecomeActiveNotification)

private fun applicationNotifications(name: NSNotificationName): Flow<Unit> = callbackFlow {
    val center: NSNotificationCenter = NSNotificationCenter.defaultCenter
    val observer: NSObjectProtocol = center.addObserverForName(
        name = name,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ -> trySend(Unit) }
    awaitClose { center.removeObserver(observer) }
}
