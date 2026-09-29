package com.emm.justchill.wiring

import com.emm.justchill.core.domain.transaction.ExportTransactionsCsvUseCase
import com.emm.justchill.feature.profile.profileModule
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

val profileWiring: Module = module {
    includes(profileModule)

    factory {
        ExportTransactionsCsvUseCase(
            transactionRepository = get(),
            clock = get(),
            zone = get(),
            formatting = Dispatchers.Default,
        )
    }
}
