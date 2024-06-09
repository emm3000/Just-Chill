package com.emm.retrofit.experiences.drinks

import com.emm.retrofit.experiences.drinks.data.DefaultDrinkRepository
import com.emm.retrofit.experiences.drinks.data.DrinkDataSource
import com.emm.retrofit.experiences.drinks.data.DrinkNetworkDataSource
import com.emm.retrofit.experiences.drinks.data.DrinkService
import com.emm.retrofit.experiences.drinks.domain.DrinkFetcher
import com.emm.retrofit.experiences.drinks.domain.DrinkRepository
import com.emm.retrofit.experiences.drinks.ui.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.create

val drinkModule = module {

    single<DrinkService> { provideDrinkService(get()) }
    single<DrinkDataSource> { DrinkNetworkDataSource(get(), get()) }
    single<DrinkRepository> { DefaultDrinkRepository(get()) }

    factoryOf(::DrinkFetcher)
    viewModelOf(::MainViewModel)
}

private fun provideDrinkService(retrofit: Retrofit): DrinkService = retrofit.create()