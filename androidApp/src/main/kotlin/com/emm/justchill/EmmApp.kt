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

    // The net for everything launched at startup, and the reason no task here guards itself: the
    // Koin lookups a task needs can throw as readily as its body — the session one opens a prefs
    // file and builds the Crashlytics logger — and an unhandled throw would reach the thread's
    // default handler and kill the launch. It reports through Log rather than DiagnosticsLogger
    // because resolving that logger is itself one of the lookups this net exists to catch.
    private val startupScope = CoroutineScope(
        SupervisorJob() + CoroutineExceptionHandler { _, error -> Log.w(TAG, "Startup task failed", error) },
    )

    override fun onCreate() {
        super.onCreate()
        val koinApp = startKoin {
            androidLogger()
            androidContext(this@EmmApp)
            // Shared module list + the Android platform module. experiencesModule is a flavor-only
            // (dev/prod) Android module, appended to the shared list.
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
