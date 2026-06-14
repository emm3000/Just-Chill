package com.emm.justchill

import com.emm.data.account.AccountLocalDataSource
import com.emm.data.account.DefaultAccountRepository
import com.emm.data.backup.DefaultBackupRepository
import com.emm.data.category.CategoryLocalDataSource
import com.emm.data.category.DefaultCategoryRepository
import com.emm.data.provideDb
import com.emm.data.provideSqlDriver
import com.emm.data.provideTransactionQueries
import com.emm.data.recurring.DefaultRecurringMovementRepository
import com.emm.data.recurring.RecurringMovementLocalDataSource
import com.emm.data.seedDefaultCategoriesIfEmpty
import com.emm.data.transaction.DefaultTransactionRepository
import com.emm.data.transaction.DefaultTransactionStatsRepository
import com.emm.data.transaction.TransactionLocalDataSource
import com.emm.data.transaction.TransactionStatsLocalDataSource
import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.ClaimLocalDataRepository
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.home.GetHomeDataUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.sync.SyncCursorStore
import com.emm.domain.sync.SyncMutex
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.hh.di.accountModule
import com.emm.justchill.hh.di.backupModule
import com.emm.justchill.hh.di.categoryModule
import com.emm.justchill.hh.di.homeModule
import com.emm.justchill.hh.di.profileModule
import com.emm.justchill.hh.di.recurringModule
import com.emm.justchill.hh.di.reportModule
import com.emm.justchill.hh.di.seetransactionsModule
import com.emm.justchill.hh.di.sharedModule
import com.emm.justchill.hh.di.transactionModule
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

// iOS :data wiring — the platform Koin module. Mirrors :app's dbModule + hhModule (the :data
// repository/datasource binds) but uses the iOS native SQLDelight driver (provideSqlDriver()
// with no Context) instead of AndroidSqliteDriver. Default-category seeding (Android's
// onCreate path) runs here once the DB is built, guarded idempotently.
//
// 5b scope: the FULL local-first closure. EXCLUDES auth/sync/supabase + Google Sign-In + Firebase
// (phase 6) — those are stubbed with the NoOp* impls below so ProfileViewModel still resolves.
private val iosDataModule = module {
    single {
        val db = provideDb(provideSqlDriver())
        // iOS equivalent of the Android driver's onCreate seed (no onCreate hook in the native
        // driver). Idempotent: only inserts when no default categories exist yet.
        seedDefaultCategoriesIfEmpty(db)
        db
    }
    single { provideTransactionQueries(get()) }

    // LocalDataSources — most take EmmDatabaseData; the transaction ones take TransactionsQueries.
    factoryOf(::CategoryLocalDataSource)
    factoryOf(::TransactionLocalDataSource)
    factoryOf(::TransactionStatsLocalDataSource)
    factoryOf(::AccountLocalDataSource)
    factoryOf(::RecurringMovementLocalDataSource)

    // Repository binds.
    factoryOf(::DefaultTransactionRepository) { bind<TransactionRepository>() }
    factoryOf(::DefaultTransactionStatsRepository) { bind<TransactionStatsRepository>() }
    factoryOf(::DefaultCategoryRepository) { bind<CategoryRepository>() }
    factoryOf(::DefaultAccountRepository) { bind<AccountRepository>() }
    factoryOf(::DefaultRecurringMovementRepository) { bind<RecurringMovementRepository>() }
    factoryOf(::DefaultBackupRepository) { bind<BackupRepository>() }

    // Home use case — Android binds this in dbModule; the iOS module set mirrors dbModule, so it
    // must bind it too or HomeViewModel (the launch screen) crashes at runtime with NoBeanDefFound.
    factoryOf(::GetHomeDataUseCase)
}

// iOS auth/sync/profile-support module. The auth USE CASES (SignOutUseCase, ObserveSessionUseCase,
// DeleteUserAccountUseCase) and SyncMutex are real domain types; they sit on top of the NoOp*
// iOS implementations of the repository ports so ProfileViewModel's closure resolves. No Supabase,
// no Google Sign-In, no AuthViewModel (those are phase 6). See IosLocalFirstStubs.kt.
private val iosProfileSupportModule = module {
    // App version surfaced in the Profile footer.
    single(named("appVersion")) { "1.0.0" }

    // No-op port implementations (phase 6 replaces with Supabase-backed ones).
    single<AuthRepository> { NoOpAuthRepository() }
    single<ClaimLocalDataRepository> { NoOpClaimLocalDataRepository() }
    single<SyncCursorStore> { NoOpSyncCursorStore() }
    single<SyncController> { NoOpSyncController() }
    single { SyncMutex() }

    // Auth use cases ProfileViewModel depends on (resolved over the no-op AuthRepository).
    factoryOf(::SignOutUseCase)
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::DeleteUserAccountUseCase)
}

// Called once from Swift at app launch (iOSApp.init). Swift sees this top-level fn as
// KoinIosKt.doInitKoin() (the `init` prefix is mangled by the Kotlin/Native Obj-C exporter).
fun initKoin() {
    startKoin {
        modules(
            // Feature modules (commonMain, :domain-only) — full local-first subset.
            transactionModule,
            seetransactionsModule,
            accountModule,
            categoryModule,
            homeModule,
            recurringModule,
            reportModule,
            profileModule,
            backupModule,
            sharedModule,
            // iOS platform wiring.
            iosDataModule,
            iosProfileSupportModule,
        )
    }
}
