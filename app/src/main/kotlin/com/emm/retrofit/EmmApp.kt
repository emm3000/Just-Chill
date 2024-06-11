package com.emm.retrofit

import android.app.Application
import com.emm.retrofit.core.coreModule
import com.emm.retrofit.experiences.drinks.drinkModule
import com.emm.retrofit.experiences.readassets.experiencesModule
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