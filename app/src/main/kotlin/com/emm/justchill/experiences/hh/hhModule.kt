package com.emm.justchill.experiences.hh

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.justchill.BuildConfig
import com.emm.justchill.CategoriesQueries
import com.emm.justchill.EmmDatabase
import com.emm.justchill.TransactionQueries
import com.emm.justchill.TransactionsCategoriesQueries
import com.emm.justchill.experiences.hh.data.AllTransactionsRetriever
import com.emm.justchill.experiences.hh.data.category.AllCategoriesRetriever
import com.emm.justchill.experiences.hh.data.category.CategoryDiskDataSource
import com.emm.justchill.experiences.hh.data.category.CategorySaver
import com.emm.justchill.experiences.hh.data.category.DefaultCategoryRepository
import com.emm.justchill.experiences.hh.data.transactioncategory.DefaultTransactionCategoryRepository
import com.emm.justchill.experiences.hh.data.transaction.DefaultTransactionRepository
import com.emm.justchill.experiences.hh.data.transactioncategory.TransactionCategoryDiskDataSource
import com.emm.justchill.experiences.hh.data.transactioncategory.TransactionCategorySaver
import com.emm.justchill.experiences.hh.data.transaction.TransactionDiskDataSource
import com.emm.justchill.experiences.hh.data.transaction.TransactionSaver
import com.emm.justchill.experiences.hh.domain.category.CategoryAdder
import com.emm.justchill.experiences.hh.domain.category.CategoryLoader
import com.emm.justchill.experiences.hh.domain.category.CategoryRepository
import com.emm.justchill.experiences.hh.domain.transaction.TransactionAdder
import com.emm.justchill.experiences.hh.domain.transactioncategory.TransactionCategoryAdder
import com.emm.justchill.experiences.hh.domain.transactioncategory.TransactionCategoryRepository
import com.emm.justchill.experiences.hh.domain.transaction.TransactionLoader
import com.emm.justchill.experiences.hh.domain.transaction.TransactionLoaderByDateRange
import com.emm.justchill.experiences.hh.domain.transaction.TransactionRepository
import com.emm.justchill.experiences.hh.presentation.category.CategoryViewModel
import com.emm.justchill.experiences.hh.presentation.home.HomeViewModel
import com.emm.justchill.experiences.hh.presentation.seetransactions.SeeTransactionsViewModel
import com.emm.justchill.experiences.hh.presentation.transaction.TransactionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.binds
import org.koin.dsl.module

val hhModule = module {

    provideSqlDelight()

    single { CategoryDiskDataSource(get(), get()) } binds arrayOf(
        CategorySaver::class,
        AllCategoriesRetriever::class
    )

    single { TransactionDiskDataSource(get(), get()) } binds arrayOf(
        TransactionSaver::class,
        AllTransactionsRetriever::class
    )

    single { TransactionCategoryDiskDataSource(get(), get()) } binds arrayOf(
        TransactionCategorySaver::class,
    )

    single<TransactionRepository> {
        DefaultTransactionRepository(get(), get())
    }

    single<CategoryRepository> {
        DefaultCategoryRepository(get(), get())
    }

    single<TransactionCategoryRepository> {
        DefaultTransactionCategoryRepository(get())
    }

    factoryOf(::CategoryAdder)
    factoryOf(::CategoryLoader)

    factoryOf(::TransactionLoader)
    factoryOf(::TransactionAdder)

    factory {
        TransactionCategoryAdder(get(), get())
    }

    factoryOf(::TransactionLoaderByDateRange)

    viewModelOf(::HomeViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::CategoryViewModel)
    viewModelOf(::SeeTransactionsViewModel)
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
        callback = object : AndroidSqliteDriver.Callback(
            schema = EmmDatabase.Schema,
        ) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }
        }
    )
}

private fun provideDb(sqlDriver: SqlDriver): EmmDatabase = EmmDatabase(sqlDriver)

private fun provideCategoryQueries(db: EmmDatabase): CategoriesQueries = db.categoriesQueries

private fun provideTransactionQueries(db: EmmDatabase): TransactionQueries = db.transactionQueries

private fun provideTransactionCategoryQueries(
    db: EmmDatabase
): TransactionsCategoriesQueries = db.transactionsCategoriesQueries