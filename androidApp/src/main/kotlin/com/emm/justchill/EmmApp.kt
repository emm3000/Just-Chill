package com.emm.justchill

import android.app.Application
import android.util.Log
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.core.androidPlatformModule
import com.emm.justchill.core.appModules
import com.emm.justchill.core.bootstrapAppGraph
import com.emm.justchill.core.platform.CurrentActivityHolder
import com.emm.justchill.core.session.KeystoreSessionManager
import com.emm.justchill.core.shortcuts.ShortcutPublisher
import com.emm.justchill.experiences.readjsonfromassets.experiencesModule
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.Koin
import org.koin.core.context.startKoin

private const val TAG = "JustChill"

class EmmApp : Application() {

    // Net for every startup task: a Koin lookup a task needs can throw as readily as its body, and
    // an unhandled throw here would reach the thread's default handler and kill the launch. It logs
    // through Log, not DiagnosticsLogger, because resolving that logger is one of the lookups this net catches.
    private val startupScope = CoroutineScope(
        SupervisorJob() + CoroutineExceptionHandler { _, error -> Log.w(TAG, "Startup task failed", error) },
    )

    override fun onCreate() {
        super.onCreate()
        val koinApp = startKoin {
            androidLogger()
            androidContext(this@EmmApp)
            modules(appModules(androidPlatformModule) + experiencesModule)
        }

        registerActivityLifecycleCallbacks(koinApp.koin.get<CurrentActivityHolder>())

        sweepLegacySession(koinApp.koin)

        bootstrapAppGraph(koinApp.koin)

        publishShortcuts(koinApp.koin)
    }

    private fun sweepLegacySession(koin: Koin) {
        startupScope.launch {
            withContext(koin.get<DispatchersProvider>().ioDispatcher) {
                koin.get<KeystoreSessionManager>().sweepLegacySession()
            }
        }
    }

    private fun publishShortcuts(koin: Koin) {
        startupScope.launch {
            withContext(koin.get<DispatchersProvider>().ioDispatcher) {
                koin.get<ShortcutPublisher>().publish()
            }
        }
    }
}
