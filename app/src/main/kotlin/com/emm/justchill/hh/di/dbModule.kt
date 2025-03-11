package com.emm.justchill.hh.di

import com.emm.data.category.provideDb
import com.emm.data.category.provideSqlDriver
import com.emm.data.category.provideTransactionQueries
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dbModule = module {

    single { provideSqlDriver(androidContext()) }
    single { provideDb(get()) }
    single { provideTransactionQueries(get()) }
}

