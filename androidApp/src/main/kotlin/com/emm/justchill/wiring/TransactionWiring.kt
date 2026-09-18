package com.emm.justchill.wiring

import com.emm.justchill.core.domain.transaction.CreateTransactionUseCase
import com.emm.justchill.core.domain.transaction.DeleteTransactionUseCase
import com.emm.justchill.core.domain.transaction.GetFrequentCombosUseCase
import com.emm.justchill.core.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.justchill.core.domain.transaction.UpdateTransactionUseCase
import com.emm.justchill.feature.transaction.capture.GetSpendShortcutCombos
import com.emm.justchill.feature.transaction.transactionModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val transactionWiring: Module = module {
    includes(transactionModule)

    factoryOf(::CreateTransactionUseCase)
    factoryOf(::UpdateTransactionUseCase)
    factoryOf(::DeleteTransactionUseCase)
    factoryOf(::GetTopUsedCategoryIdsUseCase)
    factoryOf(::GetFrequentCombosUseCase)
    factoryOf(::GetSpendShortcutCombos)
}
