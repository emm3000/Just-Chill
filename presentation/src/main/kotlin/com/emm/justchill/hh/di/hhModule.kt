package com.emm.justchill.hh.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.domain.account.AccountBalanceUpdater
import com.emm.domain.account.AccountCreator
import com.emm.domain.account.AccountDeleter
import com.emm.domain.account.AccountFinder
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpdater
import com.emm.domain.account.DailyAccountCreator
import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.UserAuthenticator
import com.emm.domain.auth.UserCreator
import com.emm.domain.category.CategoryCreator
import com.emm.domain.category.CategoryDeleter
import com.emm.domain.category.CategoryFinder
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryUpdater
import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.transaction.TransactionCreator
import com.emm.domain.transaction.TransactionDeleter
import com.emm.domain.transaction.TransactionDifferenceCalculator
import com.emm.domain.transaction.TransactionFinder
import com.emm.domain.transaction.TransactionLoader
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionSumIncome
import com.emm.domain.transaction.TransactionSumSpend
import com.emm.domain.transaction.TransactionUpdateRepository
import com.emm.domain.transaction.TransactionUpdater
import com.emm.justchill.BuildConfig
import com.emm.justchill.EmmDatabase
import com.emm.justchill.R
import com.emm.justchill.TransactionQueries
import com.emm.justchill.hh.account.data.DefaultAccountRepository
import com.emm.justchill.hh.account.presentation.AccountViewModel
import com.emm.justchill.hh.auth.presentation.LoginViewModel
import com.emm.justchill.hh.category.data.DefaultCategoryRepository
import com.emm.justchill.hh.category.presentation.CategoryViewModel
import com.emm.justchill.hh.fasttransaction.FastTransactionViewModel
import com.emm.justchill.hh.home.HomeViewModel
import com.emm.justchill.hh.shared.DefaultUniqueIdProvider
import com.emm.justchill.hh.shared.seetransactions.SeeTransactionsViewModel
import com.emm.justchill.hh.transaction.data.DefaultTransactionRepository
import com.emm.justchill.hh.transaction.data.DefaultTransactionUpdateRepository
import com.emm.justchill.hh.transaction.presentation.EditTransactionViewModel
import com.emm.justchill.hh.transaction.presentation.TransactionViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
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

    factory<TransactionUpdateRepository> {
        DefaultTransactionUpdateRepository(
            transactionQueries = get(),
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

    factoryOf(::AccountBalanceUpdater)
}