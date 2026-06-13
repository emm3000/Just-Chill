package com.emm.data

import app.cash.sqldelight.db.SqlDriver

fun provideDb(sqlDriver: SqlDriver): EmmDatabaseData = EmmDatabaseData(sqlDriver)

fun provideTransactionQueries(db: EmmDatabaseData): TransactionsQueries = db.transactionsQueries

fun provideRecurringMovementQueries(db: EmmDatabaseData): Recurring_movementsQueries = db.recurring_movementsQueries
