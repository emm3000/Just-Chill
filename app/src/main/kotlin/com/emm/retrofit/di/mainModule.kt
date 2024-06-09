package com.emm.retrofit.di

import com.emm.retrofit.experiences.drinks.data.DefaultDrinkRepository
import com.emm.retrofit.experiences.drinks.data.DrinkDataSource
import com.emm.retrofit.experiences.drinks.data.DrinkNetworkDataSource
import com.emm.retrofit.experiences.drinks.data.DrinkService
import com.emm.retrofit.experiences.drinks.domain.DrinkRepository
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.create

val mainModule = module {

    single<Retrofit> { provideDrinkService() }
    single<Dispatchers> { DefaultDispatcher() }
    single<DrinkService> { provideDrinkService(get()) }
    single<DrinkDataSource> { DrinkNetworkDataSource(get(), get()) }
    single<DrinkRepository> { DefaultDrinkRepository(get()) }
}

private fun provideDrinkService(retrofit: Retrofit): DrinkService = retrofit.create()