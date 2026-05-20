package com.emm.justchill.hh.di

import com.emm.data.account.AccountLocalDataSource
import com.emm.data.backup.DefaultBackupRepository
import com.emm.data.category.CategoryLocalDataSource
import com.emm.data.transaction.DefaultTransactionRepository
import com.emm.data.transaction.TransactionLocalDataSource
import com.emm.domain.report.GetMonthlyAmountByCategoryUseCase
import com.emm.domain.report.GetMonthlyComparisonUseCase
import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.ExportDataUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.transaction.TransactionRepository
import com.emm.justchill.hh.account.AccountsViewModel
import com.emm.justchill.hh.account.AddAccountViewModel
import com.emm.justchill.hh.category.AddCategoryViewModel
import com.emm.justchill.hh.category.CategoriesViewModel
import com.emm.justchill.hh.category.SelectCategoryViewModel
import com.emm.justchill.hh.home.HomeViewModel
import com.emm.justchill.hh.profile.ProfileViewModel
import com.emm.justchill.hh.report.ReportViewModel
import com.emm.justchill.hh.seetransactions.SeeTransactionsViewModel
import com.emm.justchill.hh.shared.DefaultUniqueIdProvider
import com.emm.justchill.hh.transaction.AddTransactionViewModel
import com.emm.justchill.hh.transaction.EditTransactionViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val hhModule = module {

    repositoriesProviders()

    factory { DateAndTimeCombiner() }
    factory { DefaultUniqueIdProvider } bind UniqueIdProvider::class

    viewModelsProviders()
    dataSource()
}

private fun Module.viewModelsProviders() {
    viewModelOf(::HomeViewModel)
    viewModelOf(::AddTransactionViewModel)
    viewModelOf(::SeeTransactionsViewModel)
    viewModelOf(::SelectCategoryViewModel)

    viewModel { parameters ->
        EditTransactionViewModel(
            transactionId = parameters.get(),
            updateTransaction = get(),
            findTransaction = get(),
            deleteTransaction = get(),
            accountRepository = get(),
            categoryRepository = get(),
            findAccount = get(),
        )
    }

    viewModel { parameters ->
        AddCategoryViewModel(
            createCategory = get(),
            initialType = parameters.get(),
            initialName = parameters.get(),
        )
    }
    viewModelOf(::CategoriesViewModel)
    viewModelOf(::AddAccountViewModel)

    viewModelOf(::AccountsViewModel)
    viewModelOf(::ReportViewModel)
    viewModelOf(::ProfileViewModel)
}


private fun Module.dataSource() {
    factoryOf(::CategoryLocalDataSource)
    factoryOf(::TransactionLocalDataSource)
    factoryOf(::AccountLocalDataSource)
}

private fun Module.repositoriesProviders() {

    factoryOf(::DefaultTransactionRepository) {
        bind<TransactionRepository>()
    }

    factoryOf(::DefaultBackupRepository) { bind<BackupRepository>() }

    factoryOf(::GetMonthlyAmountByCategoryUseCase)
    factoryOf(::GetMonthlyComparisonUseCase)
    factoryOf(::ExportDataUseCase)
    factoryOf(::ImportDataUseCase)
}