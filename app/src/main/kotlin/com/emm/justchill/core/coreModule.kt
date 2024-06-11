package com.emm.justchill.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit

val coreModule = module {

    single<Dispatchers> { DefaultDispatcher() }
    single<Retrofit> { provideDrinkService() }
    single<SharedPreferences> { provideSharedPreferences(androidContext()) }
}

private fun provideSharedPreferences(
    context: Context,
): SharedPreferences = context.getSharedPreferences(Build.ID, Context.MODE_PRIVATE)