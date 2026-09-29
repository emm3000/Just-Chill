package com.emm.justchill.core.database.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.justchill.core.database.JustChillDatabase
import kotlin.time.Clock

internal class TransactionSearchFixture {

    val driver: JdbcSqliteDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    val dataSource: TransactionLocalDataSource

    init {
        JustChillDatabase.Schema.create(driver)
        val db: JustChillDatabase = JustChillDatabase(driver)
        dataSource = TransactionLocalDataSource(db.transactionsQueries, Clock.System)
        exec("PRAGMA foreign_keys=ON")
        exec(
            "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('acc-1', 'Cuenta', 'Bank', 'PEN', 1, 1)",
        )
    }

    fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    fun insertCategory(id: String, categoryType: String = "Spend") {
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, updatedAt, createdAt) " +
                "VALUES ('$id', 'Categoria', 'icon', 'green', '$categoryType', 1, 1)",
        )
    }

    fun insertTransaction(
        id: String,
        description: String,
        occurredAt: String,
        amount: Long = 100L,
        categoryId: String? = null,
        deletedAt: Long? = null,
    ) {
        val categorySql: String = categoryId?.let { "'$it'" } ?: "NULL"
        val deletedSql: String = deletedAt?.toString() ?: "NULL"
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt, deletedAt) " +
                "VALUES ('$id', 'Spend', $amount, '$description', '$occurredAt', $categorySql, 'acc-1', 1, 1, " +
                "$deletedSql)",
        )
    }

    fun close() {
        driver.close()
    }
}
