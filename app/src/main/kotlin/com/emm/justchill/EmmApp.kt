package com.emm.justchill

import android.app.Application
import com.emm.justchill.core.coreModule
import com.emm.justchill.experiences.drinks.drinkModule
import com.emm.justchill.experiences.readjsonfromassets.experiencesModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class EmmApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@EmmApp)
            modules(
                coreModule,
                drinkModule,
                experiencesModule,
            )
        }
    }
}