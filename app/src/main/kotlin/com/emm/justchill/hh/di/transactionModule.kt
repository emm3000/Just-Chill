package com.emm.justchill.hh.di

import com.emm.domain.transaction.TransactionCreator
import com.emm.domain.transaction.TransactionDeleter
import com.emm.domain.transaction.TransactionDifferenceCalculator
import com.emm.domain.transaction.TransactionFinder
import com.emm.domain.transaction.TransactionLoader
import com.emm.domain.transaction.TransactionSumIncome
import com.emm.domain.transaction.TransactionSumSpend
import com.emm.domain.transaction.TransactionUpdater
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val transactionModule = module {
    factoryOf(::TransactionLoader)
    factoryOf(::TransactionCreator)
    factoryOf(::TransactionSumIncome)
    factoryOf(::TransactionSumSpend)
    factoryOf(::TransactionDifferenceCalculator)
    factoryOf(::TransactionFinder)
    factoryOf(::TransactionUpdater)
    factoryOf(::TransactionDeleter)
}