package com.emm.justchill.hh.di

import com.emm.data.auth.DefaultUserIdProvider
import com.emm.data.auth.UserIdProvider
import com.emm.data.provideDb
import com.emm.data.provideSqlDriver
import com.emm.data.provideTransactionQueries
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dbModule = module {

    single { provideSqlDriver(androidContext()) }
    single { provideDb(get()) }
    single { provideTransactionQueries(get()) }

    singleOf(::DefaultUserIdProvider) bind UserIdProvider::class
}

