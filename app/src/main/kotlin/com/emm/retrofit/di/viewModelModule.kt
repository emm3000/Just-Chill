package com.emm.retrofit.di

import com.emm.retrofit.experiences.drinks.domain.DrinkFetcher
import com.emm.retrofit.experiences.drinks.ui.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val viewmodelModule: Module = module {

    factoryOf(::DrinkFetcher)
    viewModelOf(::MainViewModel)
}