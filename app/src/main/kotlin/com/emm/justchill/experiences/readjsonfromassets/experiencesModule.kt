package com.emm.justchill.experiences.readjsonfromassets

import com.emm.justchill.experiences.readjsonfromassets.data.DefaultExperiencesRepository
import com.emm.justchill.experiences.readjsonfromassets.data.ExperiencesDataSource
import com.emm.justchill.experiences.readjsonfromassets.data.ExperiencesLocalDataSource
import com.emm.justchill.experiences.readjsonfromassets.domain.ExperiencesReader
import com.emm.justchill.experiences.readjsonfromassets.domain.ExperiencesRepository
import com.emm.justchill.experiences.readjsonfromassets.ui.ExperiencesViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val experiencesModule = module {

    single<ExperiencesDataSource> { ExperiencesLocalDataSource(get(), get()) }
    single<ExperiencesRepository> { DefaultExperiencesRepository(get()) }

    factoryOf(::ExperiencesReader)
    viewModelOf(::ExperiencesViewModel)
}