package com.emm.justchill

import android.app.Application
import com.emm.justchill.core.androidPlatformModule
import com.emm.justchill.core.appModules
import com.emm.justchill.core.bootstrapAppGraph
import com.emm.justchill.core.platform.CurrentActivityHolder
import com.emm.justchill.experiences.readjsonfromassets.experiencesModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class EmmApp : Application() {

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

        bootstrapAppGraph(koinApp.koin)
    }
}
