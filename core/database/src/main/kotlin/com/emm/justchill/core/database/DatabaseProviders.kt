package com.emm.justchill.core.database

import app.cash.sqldelight.db.SqlDriver

fun provideDb(sqlDriver: SqlDriver): JustChillDatabase = JustChillDatabase(sqlDriver)

fun provideTransactionQueries(db: JustChillDatabase): TransactionsQueries = db.transactionsQueries
