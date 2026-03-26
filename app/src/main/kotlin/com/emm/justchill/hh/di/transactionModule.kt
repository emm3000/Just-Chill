package com.emm.justchill.hh.di

import com.emm.domain.transaction.CreateTransactionUseCase
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.FindTransactionUseCase
import com.emm.domain.transaction.GetTransactionsUseCase
import com.emm.domain.transaction.UpdateTransactionUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val transactionModule = module {
    factoryOf(::GetTransactionsUseCase)
    factoryOf(::CreateTransactionUseCase)
    factoryOf(::FindTransactionUseCase)
    factoryOf(::UpdateTransactionUseCase)
    factoryOf(::DeleteTransactionUseCase)
}
