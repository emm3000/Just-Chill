package com.emm.justchill

import com.emm.data.category.CategoryLocalDataSource
import com.emm.data.category.DefaultCategoryRepository
import com.emm.data.provideDb
import com.emm.data.provideSqlDriver
import com.emm.data.provideTransactionQueries
import com.emm.data.transaction.DefaultTransactionRepository
import com.emm.data.transaction.TransactionLocalDataSource
import com.emm.domain.category.CategoryRepository
import com.emm.domain.transaction.TransactionRepository
import com.emm.justchill.hh.di.seetransactionsModule
import com.emm.justchill.hh.di.sharedModule
import com.emm.justchill.hh.di.transactionModule
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

// iOS :data wiring — the platform-specific Koin module. Mirrors :app's dbModule +
// the SeeTransactions slice of hhModule, but uses the iOS native SQLDelight driver
// (provideSqlDriver() with no Context) instead of AndroidSqliteDriver.
//
// Scope (5a): ONLY the bindings in SeeTransactionsViewModel's dependency closure —
// SearchTransactionsUseCase (transactionModule) -> TransactionRepository,
// CategoryRepository. No auth / sync / supabase / backup. // TODO 5b: full closure.
private val iosDataModule = module {
    single { provideSqlDriver() }
    single { provideDb(get()) }
    single { provideTransactionQueries(get()) }

    factoryOf(::TransactionLocalDataSource)
    factoryOf(::CategoryLocalDataSource)

    factoryOf(::DefaultTransactionRepository) { bind<TransactionRepository>() }
    factoryOf(::DefaultCategoryRepository) { bind<CategoryRepository>() }
}

// Called once from Swift at app launch (iOSApp.init). Swift sees this top-level
// fn as KoinIosKt.doInitKoin() (the `init` prefix is mangled by the Kotlin/Native
// Obj-C exporter to avoid clashing with Obj-C init conventions).
fun initKoin() {
    startKoin {
        modules(
            transactionModule,
            seetransactionsModule,
            sharedModule,
            iosDataModule,
        )
    }
}
