package com.emm.justchill

import android.app.Application
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.core.androidPlatformModule
import com.emm.justchill.core.appModules
import com.emm.justchill.core.bootstrapAppGraph
import com.emm.justchill.core.platform.CurrentActivityHolder
import com.emm.justchill.core.session.KeystoreSessionManager
import com.emm.justchill.experiences.readjsonfromassets.experiencesModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.Koin
import org.koin.core.context.startKoin

class EmmApp : Application() {

    private val startupScope = CoroutineScope(SupervisorJob())

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
    }

    // Off the main thread because it is prefs I/O plus a Keystore call, and off bootstrapAppGraph
    // because that runs in commonMain, where the Keystore does not exist.
    private fun sweepLegacySession(koin: Koin) {
        val dispatchers = koin.get<DispatchersProvider>()
        startupScope.launch(dispatchers.ioDispatcher) {
            koin.get<KeystoreSessionManager>().sweepLegacySession()
        }
    }
}
