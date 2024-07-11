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
import com.emm.justchill.hh.data.AllTransactionsRetriever
import com.emm.justchill.hh.data.DefaultSharedRepository
import com.emm.justchill.hh.data.SharedSqlDataSource
import com.emm.justchill.hh.data.category.AllCategoriesRetriever
import com.emm.justchill.hh.data.category.CategoryDiskDataSource
import com.emm.justchill.hh.data.category.CategorySaver
import com.emm.justchill.hh.data.category.CategorySeeder
import com.emm.justchill.hh.data.category.DefaultCategoryRepository
import com.emm.justchill.hh.data.transaction.DefaultTransactionRepository
import com.emm.justchill.hh.data.transaction.TransactionCalculator
import com.emm.justchill.hh.data.transaction.TransactionDiskDataSource
import com.emm.justchill.hh.data.transaction.TransactionSaver
import com.emm.justchill.hh.data.transaction.TransactionSeeder
import com.emm.justchill.hh.data.transactioncategory.DefaultTransactionCategoryRepository
import com.emm.justchill.hh.data.transactioncategory.TransactionCategoryDiskDataSource
import com.emm.justchill.hh.data.transactioncategory.TransactionCategoryRetriever
import com.emm.justchill.hh.data.transactioncategory.TransactionCategorySaver
import com.emm.justchill.hh.data.transactioncategory.TransactionCategorySeeder
import com.emm.justchill.hh.domain.BackupManager
import com.emm.justchill.hh.domain.DefaultBackupManager
import com.emm.justchill.hh.domain.SharedRepository
import com.emm.justchill.hh.domain.SupabaseBackupManager
import com.emm.justchill.hh.domain.TransactionModelFactory
import com.emm.justchill.hh.domain.category.CategoryAdder
import com.emm.justchill.hh.domain.category.CategoryLoader
import com.emm.justchill.hh.domain.category.CategoryRepository
import com.emm.justchill.hh.domain.transaction.TransactionAdder
import com.emm.justchill.hh.domain.transaction.TransactionDifferenceCalculator
import com.emm.justchill.hh.domain.transaction.TransactionLoader
import com.emm.justchill.hh.domain.transaction.TransactionLoaderByDateRange
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.TransactionSumIncome
import com.emm.justchill.hh.domain.transaction.TransactionSumSpend
import com.emm.justchill.hh.domain.transactioncategory.AmountDbFormatter
import com.emm.justchill.hh.domain.transactioncategory.TransactionCategoryAdder
import com.emm.justchill.hh.domain.transactioncategory.TransactionCategoryRepository
import com.emm.justchill.hh.presentation.category.CategoryViewModel
import com.emm.justchill.hh.presentation.home.HomeViewModel
import com.emm.justchill.hh.presentation.seetransactions.SeeTransactionsViewModel
import com.emm.justchill.hh.presentation.transaction.TransactionViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.binds
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

    factory(named("disk")) {
        DefaultBackupManager(
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    } bind BackupManager::class

    factory(named("supabase")) {
        SupabaseBackupManager(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    } bind BackupManager::class

    factory { TransactionModelFactory(androidApplication()) }

    viewModelsProviders()
}

private fun Module.viewModelsProviders() {
    viewModelOf(::HomeViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::CategoryViewModel)
    viewModelOf(::SeeTransactionsViewModel)
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

    single<CategoryRepository> {
        DefaultCategoryRepository(
            get(),
            get(),
            get(),
        )
    }

    single<TransactionCategoryRepository> {
        DefaultTransactionCategoryRepository(
            get(),
            get(),
            get(),
        )
    }

    single<SharedRepository> {
        DefaultSharedRepository(get())
    }

    single<SupabaseClient> { supabase(androidApplication()) }
}

private fun Module.dataSourceProviders() {
    single { CategoryDiskDataSource(get(), get()) } binds arrayOf(
        CategorySaver::class,
        AllCategoriesRetriever::class,
        CategorySeeder::class
    )
    single { TransactionDiskDataSource(get(), get()) } binds arrayOf(
        TransactionSaver::class,
        AllTransactionsRetriever::class,
        TransactionCalculator::class,
        TransactionSeeder::class
    )
    single {
        TransactionCategoryDiskDataSource(
            get(),
            get()
        )
    } binds arrayOf(
        TransactionCategorySaver::class,
        TransactionCategoryRetriever::class,
        TransactionCategorySeeder::class,
    )

    singleOf(::SharedSqlDataSource)
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
        install(Postgrest)
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
    }
}