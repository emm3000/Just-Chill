package com.emm.justchill.experiences.readassets

import com.emm.justchill.experiences.readassets.data.DefaultExperiencesRepository
import com.emm.justchill.experiences.readassets.data.ExperiencesDataSource
import com.emm.justchill.experiences.readassets.data.ExperiencesLocalDataSource
import com.emm.justchill.experiences.readassets.domain.ExperiencesReader
import com.emm.justchill.experiences.readassets.domain.ExperiencesRepository
import com.emm.justchill.experiences.readassets.ui.ExperiencesViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val experiencesModule = module {

    single<ExperiencesDataSource> { ExperiencesLocalDataSource(get(), get()) }
    single<ExperiencesRepository> { DefaultExperiencesRepository(get()) }

    factoryOf(::ExperiencesReader)
    viewModelOf(::ExperiencesViewModel)
}