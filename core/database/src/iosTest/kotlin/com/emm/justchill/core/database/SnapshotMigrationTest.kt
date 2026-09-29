package com.emm.justchill.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseFileContext
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.sqliter.withConnection
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val INSERT_ACCOUNT: String =
    "INSERT INTO accounts (accountId, name, type, currency, updatedAt, createdAt, userId, syncState) " +
        "VALUES ('account-bcp', 'BCP', 'Bank', 'PEN', 1000, 1000, 'user-1', 'Synced')"

private const val INSERT_CATEGORY: String =
    "INSERT INTO categories (categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) " +
        "VALUES ('category-salary', 'Sueldo', 'salary', 'green', 'Income', 0, 1000, 1000)"

private const val INSERT_V3_TRANSACTION: String =
    "INSERT INTO transactions (transactionId, type, amount, description, date, categoryId, accountId, " +
        "createdAt, updatedAt, userId, syncState) " +
        "VALUES ('transaction-salary', 'Income', 350000, 'Sueldo julio', 1722470400000, 'category-salary', " +
        "'account-bcp', 2000, 3000, 'user-1', 'Synced')"

private const val INSERT_TRANSACTION: String =
    "INSERT INTO transactions (transactionId, type, amount, description, occurredAt, categoryId, accountId, " +
        "createdAt, updatedAt, userId, syncState) " +
        "VALUES ('transaction-salary', 'Income', 350000, 'Sueldo julio', '2024-07-31T19:00:00', 'category-salary', " +
        "'account-bcp', 2000, 3000, 'user-1', 'Synced')"

private const val SELECT_SEEDED_TRANSACTION: String =
    "SELECT transactionId, type, amount, description, occurredAt, categoryId, accountId, " +
        "createdAt, updatedAt, userId, deletedAt, syncState FROM transactions"

private const val SELECT_COLUMNS: String =
    "SELECT m.name, p.cid, p.name, p.type, p.\"notnull\", p.dflt_value, p.pk " +
        "FROM sqlite_master m JOIN pragma_table_info(m.name) p " +
        "WHERE m.type = 'table' ORDER BY m.name, p.cid"

private const val COLUMN_ATTRIBUTES: Int = 7

private val SEEDED_TRANSACTION: List<String?> = listOf(
    "transaction-salary",
    "Income",
    "350000",
    "Sueldo julio",
    "2024-07-31T19:00:00",
    "category-salary",
    "account-bcp",
    "2000",
    "3000",
    "user-1",
    null,
    "Synced",
)

class SnapshotMigrationTest {

    private val freshName: String = "snapshot-migration-fresh.db"

    @AfterTest
    fun tearDown() {
        DatabaseFileContext.deleteDatabase(DATABASE_NAME)
        DatabaseFileContext.deleteDatabase(freshName)
    }

    @Test
    fun `a database a v3 build wrote opens at the current schema with its rows intact`() {
        installSnapshot(version = 3, seed = listOf(INSERT_ACCOUNT, INSERT_CATEGORY, INSERT_V3_TRANSACTION))

        assertOpensMigrated()
    }

    @Test
    fun `a database a v4 build wrote opens at the current schema with its rows intact`() {
        installSnapshot(version = 4, seed = listOf(INSERT_ACCOUNT, INSERT_CATEGORY, INSERT_TRANSACTION))

        assertOpensMigrated()
    }

    @Test
    fun `a database a v5 build wrote opens at the current schema with its rows intact`() {
        installSnapshot(version = 5, seed = listOf(INSERT_ACCOUNT, INSERT_CATEGORY, INSERT_TRANSACTION))

        assertOpensMigrated()
    }

    private fun installSnapshot(version: Int, seed: List<String>) {
        copySnapshot(version)
        seedAsShippedBuild(version, seed)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun copySnapshot(version: Int) {
        DatabaseFileContext.deleteDatabase(DATABASE_NAME)
        val snapshots: String = NSProcessInfo.processInfo.environment["SQLDELIGHT_SNAPSHOTS"] as? String
            ?: error("SQLDELIGHT_SNAPSHOTS is unset: run these through :core:database:iosSimulatorArm64Test")
        val copied: Boolean = NSFileManager.defaultManager.copyItemAtPath(
            srcPath = "$snapshots/$version.db",
            toPath = DatabaseFileContext.databasePath(DATABASE_NAME, null),
            error = null,
        )
        assertTrue(copied, "$snapshots/$version.db could not be copied")
    }

    private fun seedAsShippedBuild(version: Int, seed: List<String>) {
        // The committed snapshots carry user_version 0, so SQLiter's no-op create stamps `version` on the copy.
        val shipped: DatabaseConfiguration = DatabaseConfiguration(
            name = DATABASE_NAME,
            version = version,
            create = {},
            upgrade = { _, _, _ -> },
        )
        createDatabaseManager(shipped).withConnection { connection -> seed.forEach(connection::rawExecSql) }
    }

    private fun assertOpensMigrated() {
        val expectedColumns: List<List<String?>> = freshColumns()
        val driver: SqlDriver = openSqlDriver(databaseConfiguration(DATABASE_NAME))
        val version: List<List<String?>> = driver.rows("PRAGMA user_version", columns = 1)
        val columns: List<List<String?>> = driver.rows(SELECT_COLUMNS, COLUMN_ATTRIBUTES)
        val transactions: List<List<String?>> = driver.rows(SELECT_SEEDED_TRANSACTION, SEEDED_TRANSACTION.size)
        val foreignKeys: List<List<String?>> = driver.rows("PRAGMA foreign_keys", columns = 1)
        val violations: List<List<String?>> = driver.rows("PRAGMA foreign_key_check", columns = 1)
        driver.close()

        assertEquals(listOf(listOf(JustChillDatabase.Schema.version.toString())), version)
        assertEquals(expectedColumns, columns)
        assertEquals(listOf(SEEDED_TRANSACTION), transactions)
        assertEquals(listOf(listOf("1")), foreignKeys)
        assertEquals(emptyList(), violations)
    }

    private fun freshColumns(): List<List<String?>> {
        DatabaseFileContext.deleteDatabase(freshName)
        val fresh: SqlDriver = openSqlDriver(databaseConfiguration(freshName))
        val columns: List<List<String?>> = fresh.rows(SELECT_COLUMNS, COLUMN_ATTRIBUTES)
        fresh.close()
        return columns
    }

    private fun SqlDriver.rows(sql: String, columns: Int): List<List<String?>> = executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor: SqlCursor ->
            val rows: MutableList<List<String?>> = mutableListOf()
            while (cursor.next().value) {
                rows += List(columns) { index: Int -> cursor.getString(index) }
            }
            QueryResult.Value(rows.toList())
        },
        parameters = 0,
    ).value
}
