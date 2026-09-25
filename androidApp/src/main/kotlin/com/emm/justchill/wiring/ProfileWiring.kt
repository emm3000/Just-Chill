package com.emm.justchill.wiring

import com.emm.justchill.core.domain.transaction.ExportTransactionsCsvUseCase
import com.emm.justchill.feature.profile.profileModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val profileWiring: Module = module {
    includes(profileModule)

    factoryOf(::ExportTransactionsCsvUseCase)
}
