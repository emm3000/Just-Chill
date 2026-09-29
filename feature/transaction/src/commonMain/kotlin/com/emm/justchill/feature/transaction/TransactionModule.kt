package com.emm.justchill.feature.transaction

import com.emm.justchill.feature.transaction.capture.AddTransactionViewModel
import com.emm.justchill.feature.transaction.capture.EditTransactionViewModel
import com.emm.justchill.feature.transaction.list.SeeTransactionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val transactionModule: Module = module {
    viewModelOf(::AddTransactionViewModel)
    viewModelOf(::SeeTransactionsViewModel)

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
