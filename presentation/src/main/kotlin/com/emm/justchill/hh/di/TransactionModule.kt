package com.emm.justchill.hh.di

import com.emm.domain.transaction.CreateTransactionUseCase
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.GetFrequentCombosUseCase
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.domain.transaction.UpdateTransactionUseCase
import com.emm.justchill.hh.transaction.AddTransactionViewModel
import com.emm.justchill.hh.transaction.EditTransactionViewModel
import com.emm.justchill.hh.transaction.GetSpendShortcutCombos
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val transactionModule = module {
    factoryOf(::CreateTransactionUseCase)
    factoryOf(::UpdateTransactionUseCase)
    factoryOf(::DeleteTransactionUseCase)
    factoryOf(::GetTopUsedCategoryIdsUseCase)
    factoryOf(::GetFrequentCombosUseCase)
    factoryOf(::GetSpendShortcutCombos)

    viewModelOf(::AddTransactionViewModel)

    viewModel { parameters ->
        EditTransactionViewModel(
            transactionId = parameters.get(),
            updateTransaction = get(),
            transactionRepository = get(),
            deleteTransaction = get(),
            accountRepository = get(),
            categoryRepository = get(),
            getTopUsedCategoryIds = get(),
            // The parametrised DSL builds the constructor by hand, so this is passed like any
            // other dependency. It carries no default, so omitting it is a compile error rather
            // than a silent read of the device.
            todayFlow = get(),
        )
    }
}
