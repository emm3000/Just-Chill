package com.emm.justchill.hh.di

import com.emm.data.account.AccountLocalDataSource
import com.emm.data.account.DefaultAccountRepository
import com.emm.data.backup.DefaultBackupRepository
import com.emm.data.category.CategoryLocalDataSource
import com.emm.data.category.DefaultCategoryRepository
import com.emm.data.recurring.DefaultRecurringMovementRepository
import com.emm.data.recurring.RecurringMovementLocalDataSource
import com.emm.data.transaction.DefaultTransactionRepository
import com.emm.data.transaction.DefaultTransactionStatsRepository
import com.emm.data.transaction.TransactionLocalDataSource
import com.emm.data.transaction.TransactionStatsLocalDataSource
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.CategoryRepository
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionStatsRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

// :data wiring only — repository binds + LocalDataSources. Domain use cases,
// ViewModels, and cross-cutting agnostic factories moved to commonMain feature
// modules (slice 8b) because commonMain depends on :domain only, not :data.
val hhModule = module {
    repositoriesProviders()
    dataSource()
}

private fun Module.dataSource() {
    factoryOf(::CategoryLocalDataSource)
    factoryOf(::TransactionLocalDataSource)
    factoryOf(::TransactionStatsLocalDataSource)
    factoryOf(::AccountLocalDataSource)
    factoryOf(::RecurringMovementLocalDataSource)
}

private fun Module.repositoriesProviders() {
    factoryOf(::DefaultTransactionRepository) {
        bind<TransactionRepository>()
    }

    factoryOf(::DefaultTransactionStatsRepository) {
        bind<TransactionStatsRepository>()
    }

    factoryOf(::DefaultCategoryRepository) {
        bind<CategoryRepository>()
    }

    factoryOf(::DefaultAccountRepository) {
        bind<AccountRepository>()
    }

    factoryOf(::DefaultBackupRepository) { bind<BackupRepository>() }

    factoryOf(::DefaultRecurringMovementRepository) {
        bind<RecurringMovementRepository>()
    }
}
