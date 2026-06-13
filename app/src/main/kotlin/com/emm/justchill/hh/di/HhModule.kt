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
import com.emm.domain.recurring.ConfirmRecurringMovementUseCase
import com.emm.domain.recurring.CreateRecurringMovementUseCase
import com.emm.domain.recurring.DeleteRecurringMovementUseCase
import com.emm.domain.recurring.GetAllRecurringMovementDetailsUseCase
import com.emm.domain.recurring.GetPendingRecurringMovementsUseCase
import com.emm.domain.recurring.GetRecurringMonthlyTotalsUseCase
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.recurring.UpdateRecurringMovementUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.report.GetMonthlyAmountByCategoryUseCase
import com.emm.domain.report.GetMonthlyComparisonUseCase
import com.emm.domain.report.GetMonthlySectionStatsUseCase
import com.emm.domain.report.GetSavingsRateUseCase
import com.emm.domain.report.GetTopCategoriesOverMonthsUseCase
import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.ExportDataUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.justchill.hh.account.AccountsViewModel
import com.emm.justchill.hh.account.AddAccountViewModel
import com.emm.justchill.hh.category.AddCategoryViewModel
import com.emm.justchill.hh.category.CategoriesViewModel
import com.emm.justchill.hh.home.HomeViewModel
import com.emm.justchill.hh.profile.ProfileViewModel
import com.emm.justchill.hh.recurring.AddEditRecurringMovementViewModel
import com.emm.justchill.hh.recurring.RecurringMovementsViewModel
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
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val hhModule = module {

    repositoriesProviders()

    factory { DateAndTimeCombiner() }
    factory { DefaultUniqueIdProvider } bind UniqueIdProvider::class

    // Constructor-DSL deps (Koin does not use Kotlin default params): used by
    // ConfirmRecurringMovementUseCase (TimeZone) and HomeViewModel (Clock).
    // FQN avoids an ImportOrdering detekt violation (mirrors HomeViewModel).
    factory { kotlinx.datetime.TimeZone.currentSystemDefault() }
    factory<kotlin.time.Clock> { kotlin.time.Clock.System }

    viewModelsProviders()
    dataSource()
}

private fun Module.viewModelsProviders() {
    viewModelOf(::HomeViewModel)
    viewModelOf(::AddTransactionViewModel)
    viewModelOf(::SeeTransactionsViewModel)

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

    // Explicit block (not viewModelOf): appVersion is a qualified String the constructor-DSL
    // can't resolve by type. clock is omitted so it falls back to its Clock.System default.
    viewModel {
        ProfileViewModel(
            exportData = get(),
            importData = get(),
            signOut = get(),
            deleteUserAccount = get(),
            syncController = get(),
            categoryRepository = get(),
            accountRepository = get(),
            observeSession = get(),
            appVersion = get(named("appVersion")),
        )
    }

    // Slice 3 — recurring management CRUD ViewModels
    viewModelOf(::RecurringMovementsViewModel)

    viewModel { parameters ->
        AddEditRecurringMovementViewModel(
            id = parameters.getOrNull(),
            accountRepository = get(),
            categoryRepository = get(),
            recurringRepository = get(),
            createRecurring = get(),
            updateRecurring = get(),
        )
    }
}

private fun Module.dataSource() {
    factoryOf(::CategoryLocalDataSource)
    factoryOf(::TransactionLocalDataSource)
    factoryOf(::TransactionStatsLocalDataSource)
    factoryOf(::AccountLocalDataSource)
    // Slice 2 — recurring data source (Slice 3 will add more use cases + VMs)
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

    factoryOf(::GetMonthlyAmountByCategoryUseCase)
    factoryOf(::GetMonthlyComparisonUseCase)
    factoryOf(::GetMonthlySectionStatsUseCase)
    factoryOf(::GetSavingsRateUseCase)
    factoryOf(::GetTopCategoriesOverMonthsUseCase)
    factoryOf(::ExportDataUseCase)
    factoryOf(::ImportDataUseCase)

    // Slice 2 — recurring repository + use cases needed by HomeViewModel
    factoryOf(::DefaultRecurringMovementRepository) {
        bind<RecurringMovementRepository>()
    }
    factoryOf(::GetPendingRecurringMovementsUseCase)
    factoryOf(::ConfirmRecurringMovementUseCase)

    // Slice 3 — remaining recurring use cases
    factoryOf(::GetAllRecurringMovementDetailsUseCase)
    factoryOf(::GetRecurringMonthlyTotalsUseCase)
    factoryOf(::CreateRecurringMovementUseCase)
    factoryOf(::UpdateRecurringMovementUseCase)
    factoryOf(::DeleteRecurringMovementUseCase)
}
