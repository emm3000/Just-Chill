package com.emm.justchill.hh

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.justchill.BuildConfig
import com.emm.justchill.EmmDatabase
import com.emm.justchill.R
import com.emm.justchill.TransactionQueries
import com.emm.justchill.hh.data.account.AccountRemoteRepository
import com.emm.justchill.hh.data.account.AccountSupabaseRepository
import com.emm.justchill.hh.data.account.DefaultAccountRepository
import com.emm.justchill.hh.data.account.worker.AccountDeployer
import com.emm.justchill.hh.data.account.worker.AccountSyncer
import com.emm.justchill.hh.data.shared.DefaultSharedRepository
import com.emm.justchill.hh.data.shared.DefaultUniqueIdProvider
import com.emm.justchill.hh.data.shared.SharedSqlDataSource
import com.emm.justchill.hh.data.auth.DefaultAuthRepository
import com.emm.justchill.hh.data.categories.CategoryRemoteRepository
import com.emm.justchill.hh.data.categories.CategorySupabaseRepository
import com.emm.justchill.hh.data.categories.DefaultCategoryRepository
import com.emm.justchill.hh.data.categories.worker.CategoryDeployer
import com.emm.justchill.hh.data.categories.worker.CategorySyncer
import com.emm.justchill.hh.data.transaction.DefaultTransactionBackupRepository
import com.emm.justchill.hh.data.transaction.DefaultTransactionRepository
import com.emm.justchill.hh.data.transaction.DefaultTransactionUpdateRepository
import com.emm.justchill.hh.data.transaction.TransactionRemoteRepository
import com.emm.justchill.hh.data.transaction.TransactionSupabaseRepository
import com.emm.justchill.hh.data.transaction.workers.TransactionSyncer
import com.emm.justchill.hh.domain.shared.BackupManager
import com.emm.justchill.hh.domain.shared.SharedRepository
import com.emm.justchill.hh.domain.shared.SupabaseBackupManager
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.auth.UserAuthenticator
import com.emm.justchill.hh.domain.auth.UserCreator
import com.emm.justchill.hh.domain.categories.CategoryRepository
import com.emm.justchill.hh.domain.categories.crud.CategoryCreator
import com.emm.justchill.hh.domain.categories.crud.CategoryDeleter
import com.emm.justchill.hh.domain.categories.crud.CategoryFinder
import com.emm.justchill.hh.domain.categories.crud.CategoryUpdater
import com.emm.justchill.hh.domain.transaction.crud.TransactionCreator
import com.emm.justchill.hh.domain.transaction.crud.TransactionDeleter
import com.emm.justchill.hh.domain.transaction.operations.TransactionDifferenceCalculator
import com.emm.justchill.hh.domain.transaction.crud.TransactionFinder
import com.emm.justchill.hh.domain.transaction.crud.TransactionLoader
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.operations.TransactionSumIncome
import com.emm.justchill.hh.domain.transaction.operations.TransactionSumSpend
import com.emm.justchill.hh.domain.transaction.TransactionUpdateRepository
import com.emm.justchill.hh.domain.transaction.crud.TransactionUpdater
import com.emm.justchill.hh.domain.shared.DateAndTimeCombiner
import com.emm.justchill.hh.domain.transaction.TransactionBackupRepository
import com.emm.justchill.hh.data.shared.Syncer
import com.emm.justchill.hh.data.transaction.workers.TransactionDeployer
import com.emm.justchill.hh.domain.account.AccountRepository
import com.emm.justchill.hh.domain.account.crud.AccountCreator
import com.emm.justchill.hh.domain.account.crud.AccountDeleter
import com.emm.justchill.hh.domain.account.crud.AccountFinder
import com.emm.justchill.hh.domain.account.crud.AccountUpdater
import com.emm.justchill.hh.presentation.home.HomeViewModel
import com.emm.justchill.hh.presentation.auth.LoginViewModel
import com.emm.justchill.hh.presentation.seetransactions.SeeTransactionsViewModel
import com.emm.justchill.hh.presentation.transaction.EditTransactionViewModel
import com.emm.justchill.hh.presentation.transaction.TransactionViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.named
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val hhModule = module {

    provideSqlDelight()
    dataSourceProviders()
    repositoriesProviders()

    transactionsUseCases()

    factoryOf(::UserAuthenticator)
    factoryOf(::UserCreator)

    factory { DateAndTimeCombiner() }
    factory { DefaultUniqueIdProvider }

    factoryOf(::SupabaseBackupManager) bind BackupManager::class

    viewModelsProviders()
}

private fun Module.transactionsUseCases() {
    factoryOf(::TransactionLoader)
    factoryOf(::TransactionDeployer)
    factoryOf(::TransactionCreator)
    factoryOf(::TransactionSumIncome)
    factoryOf(::TransactionSumSpend)
    factoryOf(::TransactionDifferenceCalculator)
    factoryOf(::TransactionFinder)
    factoryOf(::TransactionUpdater)
    factoryOf(::TransactionDeleter)
    factoryOf(::TransactionSyncer) bind Syncer::class withOptions { named(TransactionSyncer::class.java.simpleName) }
}

private fun Module.viewModelsProviders() {
    viewModelOf(::HomeViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::SeeTransactionsViewModel)
    viewModelOf(::LoginViewModel)

    viewModel { parameters ->
        EditTransactionViewModel(
            transactionId = parameters.get(),
            transactionUpdater = get(),
            transactionFinder = get(),
            transactionDeleter = get()
        )
    }
}

private fun Module.repositoriesProviders() {

    single<TransactionBackupRepository> {
        DefaultTransactionBackupRepository(
            dispatchersProvider = get(),
            transactionsQueries = get(),
            networkDataSource = get(),
            authRepository = get(),
        )
    }

    single<TransactionRepository> {
        DefaultTransactionRepository(
            dispatchersProvider = get(),
            transactionsQueries = get(),
            syncer = get(named(TransactionSyncer::class.java.simpleName)),
        )
    }

    single<SharedRepository> {
        DefaultSharedRepository(
            get(),
        )
    }

    single<SupabaseClient> { supabase(androidApplication()) }

    factory<AuthRepository> {
        DefaultAuthRepository(get(), provideSharedPreferences(androidApplication()))
    }

    factory<TransactionUpdateRepository> {
        DefaultTransactionUpdateRepository(
            transactionQueries = get(),
            syncer = get(named(TransactionSyncer::class.java.simpleName)),
        )
    }
}

private fun provideSharedPreferences(
    context: Context,
) = context.getSharedPreferences(
    BuildConfig.APPLICATION_ID,
    Context.MODE_PRIVATE
)

private fun Module.dataSourceProviders() {

    factory<TransactionRemoteRepository> {
        TransactionSupabaseRepository(
            get(),
            get(),
        )
    }

    factoryOf(::SharedSqlDataSource)
}

private fun Module.provideSqlDelight() {
    single { provideSqlDriver(androidContext()) }
    single { provideDb(get()) }
    single { provideTransactionQueries(get()) }
}

private fun provideSqlDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(
        schema = EmmDatabase.Schema,
        context = context,
        name = "${BuildConfig.APPLICATION_ID}.db",
        callback = csm()
    )
}

private fun csm() = object : AndroidSqliteDriver.Callback(schema = EmmDatabase.Schema) {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }
}

private fun provideDb(sqlDriver: SqlDriver): EmmDatabase = EmmDatabase(sqlDriver)

private fun provideTransactionQueries(db: EmmDatabase): TransactionQueries = db.transactionQueries

private fun supabase(context: Context): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = context.getString(R.string.supabase_url),
        supabaseKey = context.getString(R.string.supabase_key)
    ) {
        install(Auth) {
            this.alwaysAutoRefresh = true
        }
        install(Postgrest)
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
    }
}

val categoryModule = module {

    factoryOf(::CategoryCreator)
    factoryOf(::CategoryDeleter)
    factoryOf(::CategoryUpdater)
    factoryOf(::CategoryFinder)
    factoryOf(::CategorySyncer) bind Syncer::class withOptions { named(CategorySyncer::class.java.simpleName) }

    factory {
        DefaultCategoryRepository(
            emmDatabase = get(),
            syncer = get(named(CategorySyncer::class.java.simpleName))
        )
    } bind CategoryRepository::class

    factoryOf(::CategorySupabaseRepository) bind CategoryRemoteRepository::class
    factoryOf(::CategoryDeployer)
}

val accountModule = module {

    factoryOf(::AccountCreator)
    factoryOf(::AccountDeleter)
    factoryOf(::AccountFinder)
    factoryOf(::AccountUpdater)
    factoryOf(::AccountSyncer) bind Syncer::class withOptions { named(AccountSyncer::class.java.simpleName) }

    factory {
        DefaultAccountRepository(
            emmDatabase = get(),
            syncer = get(named(AccountSyncer::class.java.simpleName))
        )
    } bind AccountRepository::class

    factoryOf(::AccountSupabaseRepository) bind AccountRemoteRepository::class
    factoryOf(::AccountDeployer)
}