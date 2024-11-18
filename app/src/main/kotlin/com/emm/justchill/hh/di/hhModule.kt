package com.emm.justchill.hh.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.justchill.BuildConfig
import com.emm.justchill.EmmDatabase
import com.emm.justchill.R
import com.emm.justchill.TransactionQueries
import com.emm.justchill.hh.account.data.AccountRemoteRepository
import com.emm.justchill.hh.account.data.AccountSupabaseRepository
import com.emm.justchill.hh.account.data.DefaultAccountRepository
import com.emm.justchill.hh.account.domain.AccountBalanceUpdater
import com.emm.justchill.hh.account.domain.AccountCreator
import com.emm.justchill.hh.account.domain.AccountDeleter
import com.emm.justchill.hh.account.domain.AccountFinder
import com.emm.justchill.hh.account.domain.AccountRepository
import com.emm.justchill.hh.account.domain.AccountUpdater
import com.emm.justchill.hh.account.domain.DailyAccountCreator
import com.emm.justchill.hh.account.presentation.AccountViewModel
import com.emm.justchill.hh.auth.data.DefaultAuthRepository
import com.emm.justchill.hh.auth.domain.AuthRepository
import com.emm.justchill.hh.auth.domain.UserAuthenticator
import com.emm.justchill.hh.auth.domain.UserCreator
import com.emm.justchill.hh.auth.presentation.LoginViewModel
import com.emm.justchill.hh.category.data.CategoryRemoteRepository
import com.emm.justchill.hh.category.data.CategorySupabaseRepository
import com.emm.justchill.hh.category.data.DefaultCategoryRepository
import com.emm.justchill.hh.category.domain.CategoryCreator
import com.emm.justchill.hh.category.domain.CategoryDeleter
import com.emm.justchill.hh.category.domain.CategoryFinder
import com.emm.justchill.hh.category.domain.CategoryRepository
import com.emm.justchill.hh.category.domain.CategoryUpdater
import com.emm.justchill.hh.category.presentation.CategoryViewModel
import com.emm.justchill.hh.fasttransaction.FastTransactionViewModel
import com.emm.justchill.hh.home.HomeViewModel
import com.emm.justchill.hh.shared.DateAndTimeCombiner
import com.emm.justchill.hh.shared.DefaultUniqueIdProvider
import com.emm.justchill.hh.shared.UniqueIdProvider
import com.emm.justchill.hh.shared.seetransactions.SeeTransactionsViewModel
import com.emm.justchill.hh.transaction.data.DefaultTransactionRepository
import com.emm.justchill.hh.transaction.data.DefaultTransactionUpdateRepository
import com.emm.justchill.hh.transaction.data.TransactionRemoteRepository
import com.emm.justchill.hh.transaction.data.TransactionSupabaseRepository
import com.emm.justchill.hh.transaction.domain.TransactionCreator
import com.emm.justchill.hh.transaction.domain.TransactionDeleter
import com.emm.justchill.hh.transaction.domain.TransactionDifferenceCalculator
import com.emm.justchill.hh.transaction.domain.TransactionFinder
import com.emm.justchill.hh.transaction.domain.TransactionLoader
import com.emm.justchill.hh.transaction.domain.TransactionRepository
import com.emm.justchill.hh.transaction.domain.TransactionSumIncome
import com.emm.justchill.hh.transaction.domain.TransactionSumSpend
import com.emm.justchill.hh.transaction.domain.TransactionUpdateRepository
import com.emm.justchill.hh.transaction.domain.TransactionUpdater
import com.emm.justchill.hh.transaction.presentation.EditTransactionViewModel
import com.emm.justchill.hh.transaction.presentation.TransactionViewModel
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
    factory { DefaultUniqueIdProvider } bind UniqueIdProvider::class

    viewModelsProviders()
}

private fun Module.transactionsUseCases() {
    factoryOf(::TransactionLoader)
    factoryOf(::TransactionCreator)
    factoryOf(::TransactionSumIncome)
    factoryOf(::TransactionSumSpend)
    factoryOf(::TransactionDifferenceCalculator)
    factoryOf(::TransactionFinder)
    factoryOf(::TransactionUpdater)
    factoryOf(::TransactionDeleter)
}

private fun Module.viewModelsProviders() {
    viewModelOf(::HomeViewModel)
    viewModel {
        TransactionViewModel(
            transactionCreator = get(),
            accountRepository = get()
        )
    }
    viewModelOf(::SeeTransactionsViewModel)
    viewModelOf(::LoginViewModel)

    viewModel { parameters ->
        EditTransactionViewModel(
            transactionId = parameters.get(),
            transactionUpdater = get(),
            transactionFinder = get(),
            transactionDeleter = get(),
            accountFinder = get(),
            accountRepository = get()
        )
    }

    viewModelOf(::CategoryViewModel)
    viewModelOf(::AccountViewModel)

    viewModelOf(::FastTransactionViewModel)
}

private fun Module.repositoriesProviders() {

    single<TransactionRepository> {
        DefaultTransactionRepository(
            dispatchersProvider = get(),
            transactionsQueries = get(),
        )
    }

    single<SupabaseClient> { supabase(androidApplication()) }

    factory<AuthRepository> {
        DefaultAuthRepository(get(), provideSharedPreferences(androidApplication()))
    }

    factory<TransactionUpdateRepository> {
        DefaultTransactionUpdateRepository(
            transactionQueries = get(),
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

    factory {
        DefaultCategoryRepository(
            emmDatabase = get(),
        )
    } bind CategoryRepository::class

    factoryOf(::CategorySupabaseRepository) bind CategoryRemoteRepository::class
}

val accountModule = module {

    factoryOf(::AccountCreator)
    factoryOf(::DailyAccountCreator)
    factoryOf(::AccountDeleter)
    factoryOf(::AccountFinder)
    factoryOf(::AccountUpdater)

    factory {
        DefaultAccountRepository(
            emmDatabase = get(),
        )
    } bind AccountRepository::class

    factoryOf(::AccountSupabaseRepository) bind AccountRemoteRepository::class
    factoryOf(::AccountBalanceUpdater)
}