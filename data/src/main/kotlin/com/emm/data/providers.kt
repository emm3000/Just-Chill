package com.emm.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun provideSqlDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(
        schema = EmmDatabaseData.Schema,
        context = context,
        name = "${BuildConfig.LIBRARY_PACKAGE_NAME}.db",
        callback = csm()
    )
}

fun csm() = object : AndroidSqliteDriver.Callback(schema = EmmDatabaseData.Schema) {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }
}

fun provideDb(sqlDriver: SqlDriver): EmmDatabaseData = EmmDatabaseData(sqlDriver)

fun provideTransactionQueries(db: EmmDatabaseData): TransactionQueries = db.transactionQueries