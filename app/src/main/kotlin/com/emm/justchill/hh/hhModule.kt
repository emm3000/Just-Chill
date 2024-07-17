package com.emm.justchill.hh

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.justchill.BuildConfig
import com.emm.justchill.CategoriesQueries
import com.emm.justchill.EmmDatabase
import com.emm.justchill.R
import com.emm.justchill.TransactionQueries
import com.emm.justchill.TransactionsCategoriesQueries
import com.emm.justchill.hh.data.DefaultNowProvider
import com.emm.justchill.hh.data.DefaultSharedRepository
import com.emm.justchill.hh.data.DefaultUniqueIdProvider
import com.emm.justchill.hh.data.SharedSqlDataSource
import com.emm.justchill.hh.data.auth.DefaultAuthRepository
import com.emm.justchill.hh.data.category.CategoryNetworkDataSource
import com.emm.justchill.hh.data.category.CategorySupabaseDataSource
import com.emm.justchill.hh.data.category.DefaultCategoryRepository
import com.emm.justchill.hh.data.transaction.DefaultTransactionRepository
import com.emm.justchill.hh.data.transaction.TransactionNetworkDataSource
import com.emm.justchill.hh.data.transaction.TransactionSupabaseDataSource
import com.emm.justchill.hh.data.transactioncategory.DefaultTransactionCategoryRepository
import com.emm.justchill.hh.data.transactioncategory.TransactionCategoryNetworkDataSource
import com.emm.justchill.hh.data.transactioncategory.TransactionCategorySupabaseDataSource
import com.emm.justchill.hh.domain.BackupManager
import com.emm.justchill.hh.domain.SharedRepository
import com.emm.justchill.hh.domain.SupabaseBackupManager
import com.emm.justchill.hh.domain.AndroidDataProvider
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.auth.UserAuthenticator
import com.emm.justchill.hh.domain.auth.UserCreator
import com.emm.justchill.hh.domain.category.CategoryAdder
import com.emm.justchill.hh.domain.category.CategoryLoader
import com.emm.justchill.hh.domain.category.CategoryRepository
import com.emm.justchill.hh.domain.transaction.TransactionAdder
import com.emm.justchill.hh.domain.transaction.TransactionDifferenceCalculator
import com.emm.justchill.hh.domain.transaction.TransactionFinder
import com.emm.justchill.hh.domain.transaction.TransactionLoader
import com.emm.justchill.hh.domain.transaction.TransactionLoaderByDateRange
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.TransactionSumIncome
import com.emm.justchill.hh.domain.transaction.TransactionSumSpend
import com.emm.justchill.hh.domain.transaction.TransactionUpdater
import com.emm.justchill.hh.domain.transactioncategory.AmountDbFormatter
import com.emm.justchill.hh.domain.transactioncategory.TransactionCategoryAdder
import com.emm.justchill.hh.domain.transactioncategory.TransactionCategoryRepository
import com.emm.justchill.hh.presentation.category.CategoryViewModel
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

    factoryOf(::CategoryAdder)
    factoryOf(::CategoryLoader)
    factoryOf(::TransactionLoader)
    factoryOf(::TransactionAdder)

    factory {
        TransactionCategoryAdder(get(), get(), AmountDbFormatter())
    }

    factoryOf(::TransactionLoaderByDateRange)

    factoryOf(::TransactionSumIncome)
    factoryOf(::TransactionSumSpend)
    factoryOf(::TransactionDifferenceCalculator)

    factoryOf(::UserAuthenticator)
    factoryOf(::UserCreator)
    factoryOf(::TransactionFinder)
    factoryOf(::TransactionUpdater)

    factory<BackupManager> {
        SupabaseBackupManager(
            get(),
            get(),
            get(),
            get(),
            get(),
            androidApplication(),
        )
    }

    factory { AndroidDataProvider(androidApplication()) }

    viewModelsProviders()
}

private fun Module.viewModelsProviders() {
    viewModelOf(::HomeViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::CategoryViewModel)
    viewModelOf(::SeeTransactionsViewModel)
    viewModelOf(::LoginViewModel)

    viewModel { parameters ->
        EditTransactionViewModel(
            categoryLoader = get(),
            transactionId = parameters.get(),
            transactionUpdater = get(),
            transactionFinder = get(),
            amountCleaner = AmountDbFormatter(),
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
            get(),
        )
    }

    single<CategoryRepository> {
        DefaultCategoryRepository(
            get(),
            get(),
            get(),
            DefaultNowProvider,
            DefaultUniqueIdProvider,
            get(),
            get()
        )
    }

    single<TransactionCategoryRepository> {
        DefaultTransactionCategoryRepository(
            get(),
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
        DefaultAuthRepository(get())
    }
}

private fun Module.dataSourceProviders() {
    factory<CategoryNetworkDataSource> {
        CategorySupabaseDataSource(
            get(),
            get(),
        )
    }
    factory<TransactionNetworkDataSource> {
        TransactionSupabaseDataSource(
            get(),
            get(),
        )
    }
    factory<TransactionCategoryNetworkDataSource> {
        TransactionCategorySupabaseDataSource(
            get(),
            get(),
        )
    }
    factoryOf(::SharedSqlDataSource)
}

private fun Module.provideSqlDelight() {
    single { provideSqlDriver(androidContext()) }
    single { provideDb(get()) }
    single { provideCategoryQueries(get()) }
    single { provideTransactionQueries(get()) }
    single { provideTransactionCategoryQueries(get()) }
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

private fun provideCategoryQueries(db: EmmDatabase): CategoriesQueries = db.categoriesQueries

private fun provideTransactionQueries(db: EmmDatabase): TransactionQueries = db.transactionQueries

private fun provideTransactionCategoryQueries(
    db: EmmDatabase
): TransactionsCategoriesQueries = db.transactionsCategoriesQueries

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