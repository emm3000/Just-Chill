package com.emm.justchill.hh

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.justchill.BuildConfig
import com.emm.justchill.EmmDatabase
import com.emm.justchill.R
import com.emm.justchill.TransactionQueries
import com.emm.justchill.hh.data.DefaultSharedRepository
import com.emm.justchill.hh.data.SharedSqlDataSource
import com.emm.justchill.hh.data.auth.DefaultAuthRepository
import com.emm.justchill.hh.data.transaction.DefaultTransactionRepository
import com.emm.justchill.hh.data.transaction.DefaultTransactionUpdateRepository
import com.emm.justchill.hh.domain.transaction.remote.TransactionSupabaseRepository
import com.emm.justchill.hh.data.transaction.DefaultTransactionSupabaseRepository
import com.emm.justchill.hh.domain.BackupManager
import com.emm.justchill.hh.domain.SharedRepository
import com.emm.justchill.hh.domain.SupabaseBackupManager
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.auth.UserAuthenticator
import com.emm.justchill.hh.domain.auth.UserCreator
import com.emm.justchill.hh.domain.transaction.TransactionAdder
import com.emm.justchill.hh.domain.transaction.TransactionDeleter
import com.emm.justchill.hh.domain.transaction.TransactionDifferenceCalculator
import com.emm.justchill.hh.domain.transaction.TransactionFinder
import com.emm.justchill.hh.domain.transaction.TransactionLoader
import com.emm.justchill.hh.domain.transaction.TransactionLoaderByDateRange
import com.emm.justchill.hh.domain.transaction.TransactionLoaderByDateRangeWithPage
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.TransactionSumIncome
import com.emm.justchill.hh.domain.transaction.TransactionSumSpend
import com.emm.justchill.hh.domain.transaction.TransactionUpdateRepository
import com.emm.justchill.hh.domain.transaction.TransactionUpdater
import com.emm.justchill.hh.domain.AmountDbFormatter
import com.emm.justchill.hh.domain.DateAndTimeCombiner
import com.emm.justchill.hh.domain.transaction.remote.TransactionAdderResolver
import com.emm.justchill.hh.domain.transaction.remote.TransactionDeployer
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
import org.koin.dsl.module

val hhModule = module {

    provideSqlDelight()
    dataSourceProviders()
    repositoriesProviders()

    factoryOf(::TransactionLoader)

    factory {
        TransactionAdderResolver(androidApplication())
    }
    factory {
        TransactionDeployer(
            get(), get(), get()
        )
    }
    factory {
        TransactionAdder(
            repository = get(),
            dateAndTimeCombiner = DateAndTimeCombiner(),
            transactionAdderResolver = get()
        )
    }

    factoryOf(::TransactionLoaderByDateRange)
    factoryOf(::TransactionLoaderByDateRangeWithPage)

    factoryOf(::TransactionSumIncome)
    factoryOf(::TransactionSumSpend)
    factoryOf(::TransactionDifferenceCalculator)

    factoryOf(::UserAuthenticator)
    factoryOf(::UserCreator)
    factoryOf(::TransactionFinder)
    factory {
        TransactionUpdater(get(), DateAndTimeCombiner())
    }
    factoryOf(::TransactionDeleter)

    factory<BackupManager> {
        SupabaseBackupManager(
            get(),
            get(),
            get(),
        )
    }

    viewModelsProviders()
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
            amountCleaner = AmountDbFormatter(),
            transactionDeleter = get()
        )
    }
}

private fun Module.repositoriesProviders() {
    single<TransactionRepository> {
        DefaultTransactionRepository(
            get(),
            get(),
            get(),
            get(),
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
            get(),
        )
    }
}

private fun provideSharedPreferences(
    context: Context
) = context.getSharedPreferences(
    BuildConfig.APPLICATION_ID,
    Context.MODE_PRIVATE
)

private fun Module.dataSourceProviders() {

    factory<TransactionSupabaseRepository> {
        DefaultTransactionSupabaseRepository(
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
        install(Auth)
        install(Postgrest)
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
    }
}