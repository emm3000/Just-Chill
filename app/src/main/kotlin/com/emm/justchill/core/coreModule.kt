package com.emm.justchill.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.emm.justchill.core.preferences.AppPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {

    single<DispatchersProvider> { DefaultDispatcher() }
    single<SharedPreferences> { provideSharedPreferences(androidContext()) }
    single { AppPreferences(get()) }
}

private fun provideSharedPreferences(context: Context): SharedPreferences =
    context.getSharedPreferences(Build.ID, Context.MODE_PRIVATE)
