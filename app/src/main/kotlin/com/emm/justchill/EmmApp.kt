package com.emm.justchill

import android.app.Application
import com.emm.justchill.core.coreModule
import com.emm.justchill.experiences.readjsonfromassets.experiencesModule
import com.emm.justchill.hh.di.accountModule
import com.emm.justchill.hh.di.categoryModule
import com.emm.justchill.hh.di.dbModule
import com.emm.justchill.hh.di.hhModule
import com.emm.justchill.hh.di.supabaseModule
import com.emm.justchill.hh.di.transactionModule
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
                experiencesModule,
                hhModule,
                categoryModule,
                accountModule,
                transactionModule,
                dbModule,
                supabaseModule,
            )
        }
    }
}