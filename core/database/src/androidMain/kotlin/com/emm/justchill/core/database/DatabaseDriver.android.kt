package com.emm.justchill.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun provideSqlDriver(context: Context): SqlDriver = AndroidSqliteDriver(
    schema = JustChillDatabase.Schema,
    context = context,
    name = DATABASE_NAME,
    callback = csm(),
)

fun csm(): AndroidSqliteDriver.Callback = object : AndroidSqliteDriver.Callback(schema = JustChillDatabase.Schema) {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL(SEED_DEFAULT_CATEGORIES)
    }
}
