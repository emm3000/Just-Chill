package com.emm.justchill.hh.di

import com.emm.domain.transaction.CreateTransactionUseCase
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.FindTransactionUseCase
import com.emm.domain.transaction.GetFrequentCombosUseCase
import com.emm.domain.transaction.GetLastUsedAccountIdUseCase
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.domain.transaction.GetTransactionsUseCase
import com.emm.domain.transaction.SearchTransactionsUseCase
import com.emm.domain.transaction.UpdateTransactionUseCase
import com.emm.justchill.hh.transaction.AddTransactionViewModel
import com.emm.justchill.hh.transaction.EditTransactionViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val transactionModule = module {
    factoryOf(::GetTransactionsUseCase)
    factoryOf(::CreateTransactionUseCase)
    factoryOf(::FindTransactionUseCase)
    factoryOf(::UpdateTransactionUseCase)
    factoryOf(::DeleteTransactionUseCase)
    factoryOf(::SearchTransactionsUseCase)
    factoryOf(::GetTopUsedCategoryIdsUseCase)
    factoryOf(::GetFrequentCombosUseCase)
    factoryOf(::GetLastUsedAccountIdUseCase)

    viewModelOf(::AddTransactionViewModel)

    viewModel { parameters ->
        EditTransactionViewModel(
            transactionId = parameters.get(),
            updateTransaction = get(),
            findTransaction = get(),
            deleteTransaction = get(),
            accountRepository = get(),
            categoryRepository = get(),
            findAccount = get(),
            getTopUsedCategoryIds = get(),
            // Explicit because the parametrised DSL builds the constructor by hand: Koin never
            // applies Kotlin default arguments, so an omitted clock would not fall back — it
            // would not compile, and a defaulted zone would silently ignore sharedModule's.
            clock = get(),
            zone = get(),
        )
    }
}
