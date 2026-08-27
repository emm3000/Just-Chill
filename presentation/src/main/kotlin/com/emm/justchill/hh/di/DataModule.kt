package com.emm.justchill.hh.di

import com.emm.data.account.AccountLocalDataSource
import com.emm.data.account.DefaultAccountRepository
import com.emm.data.backup.DefaultBackupEraser
import com.emm.data.backup.DefaultBackupPruner
import com.emm.data.backup.DefaultBackupRepository
import com.emm.data.backup.DefaultBackupUploader
import com.emm.data.backup.DefaultBackupVerifier
import com.emm.data.category.CategoryLocalDataSource
import com.emm.data.category.DefaultCategoryRepository
import com.emm.data.loan.DefaultLoanPaymentRepository
import com.emm.data.loan.DefaultLoanRepository
import com.emm.data.loan.LoanLocalDataSource
import com.emm.data.loan.LoanPaymentLocalDataSource
import com.emm.data.provideTransactionQueries
import com.emm.data.recurring.DefaultRecurringMovementRepository
import com.emm.data.recurring.RecurringMovementLocalDataSource
import com.emm.data.transaction.DefaultTransactionRepository
import com.emm.data.transaction.DefaultTransactionStatsRepository
import com.emm.data.transaction.TransactionLocalDataSource
import com.emm.data.transaction.TransactionStatsLocalDataSource
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.CategoryRepository
import com.emm.domain.loan.LoanPaymentRepository
import com.emm.domain.loan.LoanRepository
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.backup.BackupEraser
import com.emm.domain.shared.backup.BackupPruner
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.BackupUploader
import com.emm.domain.shared.backup.BackupVerifier
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionStatsRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

// The platform-specific DB single (driver construction + provideDb) does NOT live here — it needs
// androidContext(), so it stays in androidPlatformModule.
val dataModule = module {
    single { provideTransactionQueries(get()) }

    // LocalDataSources — most take EmmDatabaseData; the transaction ones take TransactionsQueries.
    factoryOf(::CategoryLocalDataSource)
    factoryOf(::TransactionLocalDataSource)
    factoryOf(::TransactionStatsLocalDataSource)
    factoryOf(::AccountLocalDataSource)
    factoryOf(::RecurringMovementLocalDataSource)
    factoryOf(::LoanLocalDataSource)
    factoryOf(::LoanPaymentLocalDataSource)

    // Repository binds.
    factoryOf(::DefaultTransactionRepository) { bind<TransactionRepository>() }
    factoryOf(::DefaultTransactionStatsRepository) { bind<TransactionStatsRepository>() }
    factoryOf(::DefaultCategoryRepository) { bind<CategoryRepository>() }
    factoryOf(::DefaultAccountRepository) { bind<AccountRepository>() }
    factoryOf(::DefaultRecurringMovementRepository) { bind<RecurringMovementRepository>() }
    factoryOf(::DefaultLoanRepository) { bind<LoanRepository>() }
    factoryOf(::DefaultLoanPaymentRepository) { bind<LoanPaymentRepository>() }
    factoryOf(::DefaultBackupRepository) { bind<BackupRepository>() }

    // Written out rather than `factoryOf(::DefaultBackupUploader)`: the class has a second,
    // `internal` constructor taking its storage seam, which only :data (and its tests) can see. The
    // constructor DSL would have to resolve a reference this module cannot name. Same for the
    // verifier and the eraser, and for the pruner, which additionally takes the graph's Clock and TimeZone —
    // spelled as get() because neither carries a default any more (docs/CODE_QUALITY.md, Dates), and
    // AppGraphKoinTest asserts by identity that this block really passed the bound instances rather
    // than its own.
    factory<BackupUploader> { DefaultBackupUploader(get()) }
    factory<BackupVerifier> { DefaultBackupVerifier(get()) }
    factory<BackupPruner> { DefaultBackupPruner(get(), get(), get()) }
    factory<BackupEraser> { DefaultBackupEraser(get()) }
}
