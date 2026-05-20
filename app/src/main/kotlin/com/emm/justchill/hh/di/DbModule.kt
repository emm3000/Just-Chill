package com.emm.justchill.hh.di

import com.emm.data.provideDb
import com.emm.data.provideSqlDriver
import com.emm.data.provideTransactionQueries
import com.emm.domain.home.GetHomeDataUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import kotlin.time.Clock

val dbModule = module {

    single { provideSqlDriver(androidContext()) }
    single { provideDb(get()) }
    single { provideTransactionQueries(get()) }

    single<Clock> { Clock.System }
    factoryOf(::GetHomeDataUseCase)
}
