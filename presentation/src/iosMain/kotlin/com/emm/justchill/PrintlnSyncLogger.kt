package com.emm.justchill

import com.emm.domain.sync.SyncLogger

private const val TAG = "JustChillSync"

/**
 * iOS [SyncLogger]. `println` reaches the Xcode console and the device log through Kotlin/Native's
 * stdout bridge, which is all iOS gets: there is no Crashlytics on this platform (see
 * `docs/adr/003` — iOS is frozen at "it compiles", with no users and no crash-reporting SDK wired).
 *
 * Never throws, per the port contract — `println` cannot fail here.
 */
internal class PrintlnSyncLogger : SyncLogger {
    override fun warn(message: String, throwable: Throwable?) {
        println("$TAG: $message${throwable?.let { " | ${it::class.simpleName}: ${it.message}" }.orEmpty()}")
    }
}
