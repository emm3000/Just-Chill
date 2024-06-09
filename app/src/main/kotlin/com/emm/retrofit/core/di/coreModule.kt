package com.emm.retrofit.core.di

import com.emm.retrofit.core.DefaultDispatcher
import com.emm.retrofit.core.Dispatchers
import com.emm.retrofit.core.provideDrinkService
import org.koin.dsl.module
import retrofit2.Retrofit

val coreModule = module {

    single<Dispatchers> { DefaultDispatcher() }
    single<Retrofit> { provideDrinkService() }
}