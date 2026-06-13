package com.emm.justchill.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.emm.justchill.BuildConfig
import com.emm.justchill.core.platform.CurrentActivityHolder
import com.emm.justchill.core.preferences.AppPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreModule = module {

    single<DispatchersProvider> { DefaultDispatcher() }
    single<SharedPreferences> { provideSharedPreferences(androidContext()) }
    single { AppPreferences(get()) }
    single { CurrentActivityHolder() }

    // Platform-provided app version (no BuildConfig in commonMain). Consumed by ProfileViewModel
    // via the "appVersion" qualifier; stamped into exported backups.
    single(named("appVersion")) { BuildConfig.VERSION_NAME }
}

private fun provideSharedPreferences(context: Context): SharedPreferences =
    context.getSharedPreferences(Build.ID, Context.MODE_PRIVATE)
