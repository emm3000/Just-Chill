package com.emm.justchill.experiences.drinks

import com.emm.justchill.experiences.drinks.data.DefaultDrinkRepository
import com.emm.justchill.experiences.drinks.data.DrinkDiskDataSource
import com.emm.justchill.experiences.drinks.data.DrinkFetcher
import com.emm.justchill.experiences.drinks.data.DrinkNetworkDataSource
import com.emm.justchill.experiences.drinks.data.DrinkSaver
import com.emm.justchill.experiences.drinks.data.DrinkService
import com.emm.justchill.experiences.drinks.domain.DrinkFetcher as DomainDrinkFetcher
import com.emm.justchill.experiences.drinks.domain.DrinkRepository
import com.emm.justchill.experiences.drinks.ui.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.create

val drinkModule = module {

    single<DrinkService> { provideDrinkService(get()) }

    drinkNetworkDataSource()
    drinkLocalDataSource()
    drinkRepository()

    factoryOf(::DomainDrinkFetcher)
    viewModelOf(::MainViewModel)
}

private fun Module.drinkRepository() {
    single<DrinkRepository> {
        DefaultDrinkRepository(
            get(named("network")),
            get(named("local")),
            get(named("local")),
        )
    }
}

private fun Module.drinkNetworkDataSource() {
    single(named("network")) {
        DrinkNetworkDataSource(get(), get())
    } bind DrinkFetcher::class
}

private fun Module.drinkLocalDataSource() {
    single(named("local")) {
        DrinkDiskDataSource(
            get(),
            get()
        )
    } binds arrayOf(DrinkFetcher::class, DrinkSaver::class)
}

private fun provideDrinkService(retrofit: Retrofit): DrinkService = retrofit.create()