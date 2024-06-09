package com.emm.retrofit.experiences.readassets

import com.emm.retrofit.experiences.readassets.data.DefaultExperiencesRepository
import com.emm.retrofit.experiences.readassets.data.ExperiencesDataSource
import com.emm.retrofit.experiences.readassets.data.ExperiencesLocalDataSource
import com.emm.retrofit.experiences.readassets.domain.ExperiencesReader
import com.emm.retrofit.experiences.readassets.domain.ExperiencesRepository
import com.emm.retrofit.experiences.readassets.ui.ExperiencesViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val experiencesModule = module {

    single<ExperiencesDataSource> { ExperiencesLocalDataSource(get(), get()) }
    single<ExperiencesRepository> { DefaultExperiencesRepository(get()) }

    factoryOf(::ExperiencesReader)
    viewModelOf(::ExperiencesViewModel)
}