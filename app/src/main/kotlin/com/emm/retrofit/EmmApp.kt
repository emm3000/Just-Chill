package com.emm.retrofit

import android.app.Application
import com.emm.retrofit.di.mainModule
import com.emm.retrofit.di.viewmodelModule
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
                mainModule,
                viewmodelModule
            )
        }
    }
}