package com.emm.justchill.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseFileContext
import co.touchlab.sqliter.longForQuery
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ForeignKeyOrderTest {

    private val databaseName: String = perRunDatabaseName("foreign-key-order")
    private val shipped: DatabaseConfiguration = databaseConfiguration(databaseName)
    private var foreignKeysDuringUpgrade: Long? = null

    private val nextRelease: DatabaseConfiguration = shipped.copy(
        version = shipped.version + 1,
        upgrade = { connection, oldVersion, newVersion ->
            foreignKeysDuringUpgrade = connection.longForQuery("PRAGMA foreign_keys")
            shipped.upgrade(connection, oldVersion, newVersion)
        },
    )

    @BeforeTest
    fun setUp() {
        openAndMigrate(shipped)
    }

    @AfterTest
    fun tearDown() {
        DatabaseFileContext.deleteDatabase(databaseName)
    }

    @Test
    fun `foreign keys are off while the upgrade chain runs`() {
        openAndMigrate(nextRelease)

        assertEquals(0L, foreignKeysDuringUpgrade)
    }

    @Test
    fun `foreign keys are on for reads and writes once the upgraded database is open`() {
        val driver: SqlDriver = openSqlDriver(nextRelease)
        val onReader: Long = driver.foreignKeys()
        val onWriter: Long = JustChillDatabase(driver).transactionWithResult { driver.foreignKeys() }
        driver.close()

        assertEquals(listOf(1L, 1L), listOf(onReader, onWriter))
    }

    private fun openAndMigrate(configuration: DatabaseConfiguration) {
        val driver: SqlDriver = openSqlDriver(configuration)
        driver.foreignKeys()
        driver.close()
    }

    private fun SqlDriver.foreignKeys(): Long = executeQuery(
        identifier = null,
        sql = "PRAGMA foreign_keys",
        mapper = { cursor: SqlCursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: -1L)
        },
        parameters = 0,
    ).value
}
