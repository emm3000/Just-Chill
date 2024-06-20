package com.emm.justchill.experiences.hh

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.justchill.BuildConfig
import com.emm.justchill.CategoriesQueries
import com.emm.justchill.EmmDatabase
import com.emm.justchill.TransactionQueries
import com.emm.justchill.experiences.hh.data.AllItemsRetriever
import com.emm.justchill.experiences.hh.data.CategoryDiskDataSource
import com.emm.justchill.experiences.hh.data.CategorySaver
import com.emm.justchill.experiences.hh.data.DefaultCategoryRepository
import com.emm.justchill.experiences.hh.data.DefaultTransactionRepository
import com.emm.justchill.experiences.hh.data.TransactionDiskDataSource
import com.emm.justchill.experiences.hh.data.TransactionSaver
import com.emm.justchill.experiences.hh.domain.CategoryAdder
import com.emm.justchill.experiences.hh.domain.CategoryLoader
import com.emm.justchill.experiences.hh.domain.CategoryRepository
import com.emm.justchill.experiences.hh.domain.TransactionAdder
import com.emm.justchill.experiences.hh.domain.TransactionLoader
import com.emm.justchill.experiences.hh.domain.TransactionRepository
import com.emm.justchill.experiences.hh.presentation.HomeViewModel
import com.emm.justchill.experiences.hh.presentation.IncomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

val hhModule = module {
    single { provideSqlDriver(androidContext()) }
    single { provideDb(get()) }
    single { provideCategoryQueries(get()) }
    single { provideTransactionQueries(get()) }

    single(named("category")) { CategoryDiskDataSource(get(), get()) } binds arrayOf(
        CategorySaver::class,
        AllItemsRetriever::class
    )

    single(named("transaction")) { TransactionDiskDataSource(get(), get()) } binds arrayOf(
        TransactionSaver::class,
        AllItemsRetriever::class
    )

    single<TransactionRepository> {
        DefaultTransactionRepository(get(named("transaction")), get(named("transaction")))
    }

    single<CategoryRepository> {
        DefaultCategoryRepository(get(named("category")), get(named("category")))
    }

    factoryOf(::CategoryAdder)
    factoryOf(::CategoryLoader)

    factoryOf(::TransactionLoader)
    factoryOf(::TransactionAdder)

    viewModelOf(::HomeViewModel)
    viewModelOf(::IncomeViewModel)
}

private fun provideSqlDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(
        schema = EmmDatabase.Schema,
        context = context,
        name = "${BuildConfig.APPLICATION_ID}.db",
        callback = object : AndroidSqliteDriver.Callback(EmmDatabase.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }
        }
    )
}

private fun provideDb(sqlDriver: SqlDriver): EmmDatabase = EmmDatabase(sqlDriver)

private fun provideCategoryQueries(db: EmmDatabase): CategoriesQueries = db.categoriesQueries

private fun provideTransactionQueries(db: EmmDatabase): TransactionQueries = db.transactionQueries