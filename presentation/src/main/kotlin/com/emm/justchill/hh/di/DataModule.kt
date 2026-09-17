package com.emm.justchill.hh.di

import com.emm.justchill.core.database.account.AccountLocalDataSource
import com.emm.justchill.core.database.account.DefaultAccountRepository
import com.emm.justchill.core.database.backup.DefaultBackupEraser
import com.emm.justchill.core.database.backup.DefaultBackupPruner
import com.emm.justchill.core.database.backup.DefaultBackupRepository
import com.emm.justchill.core.database.backup.DefaultBackupUploader
import com.emm.justchill.core.database.backup.DefaultBackupVerifier
import com.emm.justchill.core.database.category.CategoryLocalDataSource
import com.emm.justchill.core.database.category.DefaultCategoryRepository
import com.emm.justchill.core.database.loan.DefaultLoanPaymentRepository
import com.emm.justchill.core.database.loan.DefaultLoanRepository
import com.emm.justchill.core.database.loan.LoanLocalDataSource
import com.emm.justchill.core.database.loan.LoanPaymentLocalDataSource
import com.emm.justchill.core.database.provideTransactionQueries
import com.emm.justchill.core.database.recurring.DefaultRecurringMovementRepository
import com.emm.justchill.core.database.recurring.RecurringMovementLocalDataSource
import com.emm.justchill.core.database.transaction.DefaultTransactionRepository
import com.emm.justchill.core.database.transaction.DefaultTransactionStatsRepository
import com.emm.justchill.core.database.transaction.TransactionLocalDataSource
import com.emm.justchill.core.database.transaction.TransactionStatsLocalDataSource
import com.emm.justchill.core.domain.account.AccountRepository
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.loan.LoanPaymentRepository
import com.emm.justchill.core.domain.loan.LoanRepository
import com.emm.justchill.core.domain.recurring.RecurringMovementRepository
import com.emm.justchill.core.domain.shared.backup.BackupEraser
import com.emm.justchill.core.domain.shared.backup.BackupPruner
import com.emm.justchill.core.domain.shared.backup.BackupRepository
import com.emm.justchill.core.domain.shared.backup.BackupUploader
import com.emm.justchill.core.domain.shared.backup.BackupVerifier
import com.emm.justchill.core.domain.transaction.TransactionRepository
import com.emm.justchill.core.domain.transaction.TransactionStatsRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

// The platform-specific DB single (driver construction + provideDb) does NOT live here — it needs
// androidContext(), so it stays in androidPlatformModule.
val dataModule = module {
    single { provideTransactionQueries(get()) }

    factoryOf(::CategoryLocalDataSource)
    factoryOf(::TransactionLocalDataSource)
    factoryOf(::TransactionStatsLocalDataSource)
    factoryOf(::AccountLocalDataSource)
    factoryOf(::RecurringMovementLocalDataSource)
    factoryOf(::LoanLocalDataSource)
    factoryOf(::LoanPaymentLocalDataSource)

    factoryOf(::DefaultTransactionRepository) { bind<TransactionRepository>() }
    factoryOf(::DefaultTransactionStatsRepository) { bind<TransactionStatsRepository>() }
    factoryOf(::DefaultCategoryRepository) { bind<CategoryRepository>() }
    factoryOf(::DefaultAccountRepository) { bind<AccountRepository>() }
    factoryOf(::DefaultRecurringMovementRepository) { bind<RecurringMovementRepository>() }
    factoryOf(::DefaultLoanRepository) { bind<LoanRepository>() }
    factoryOf(::DefaultLoanPaymentRepository) { bind<LoanPaymentRepository>() }
    factoryOf(::DefaultBackupRepository) { bind<BackupRepository>() }

    // Written out rather than factoryOf(::DefaultBackupUploader): the class has a second, internal
    // constructor taking its storage seam, which only :core:database (and its tests) can see, and the
    // constructor DSL would have to resolve a reference this module cannot name.
    factory<BackupUploader> { DefaultBackupUploader(get()) }
    factory<BackupVerifier> { DefaultBackupVerifier(get()) }
    factory<BackupPruner> { DefaultBackupPruner(get(), get(), get()) }
    factory<BackupEraser> { DefaultBackupEraser(get()) }
}
