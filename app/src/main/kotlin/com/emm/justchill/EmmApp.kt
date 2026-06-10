package com.emm.justchill

import android.app.Application
import com.emm.domain.auth.ClaimLocalDataOnAuthenticationUseCase
import com.emm.justchill.core.coreModule
import com.emm.justchill.experiences.readjsonfromassets.experiencesModule
import com.emm.justchill.hh.di.accountModule
import com.emm.justchill.hh.di.authModule
import com.emm.justchill.hh.di.categoryModule
import com.emm.justchill.hh.di.dbModule
import com.emm.justchill.hh.di.hhModule
import com.emm.justchill.hh.di.supabaseModule
import com.emm.justchill.hh.di.transactionModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class EmmApp : Application() {

    // Lives for the whole process: drives the global session → claim observer.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
                authModule,
            )
        }

        val claimOnAuthentication = get<ClaimLocalDataOnAuthenticationUseCase>()
        appScope.launch { claimOnAuthentication() }
    }
}
