package com.emm.data.sync

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.EmmDatabaseData
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins the exception contract the sync pull relies on, against the REAL Android SQLite driver.
 *
 * BaseTableSync.applyRemoteRow catches [SQLiteConstraintException] to skip orphan child rows
 * (FK parent not yet pulled) without aborting the table's transaction. JVM unit tests cannot
 * verify this — the JDBC driver throws java.sql.SQLException instead — so this instrumented
 * test asserts:
 *  1. an orphan FK insert via insertOrIgnoreFromRemote throws exactly SQLiteConstraintException
 *     (ON CONFLICT clauses do not apply to FOREIGN KEY constraints), and
 *  2. catching it inside db.transaction {} still commits sibling rows applied in the same
 *     transaction (statement-level ABORT, not transaction rollback).
 *
 * Run with: ./gradlew :data:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SyncFkExceptionTest {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: EmmDatabaseData

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        driver = AndroidSqliteDriver(
            schema = EmmDatabaseData.Schema,
            context = context,
            name = null, // in-memory
            callback = object : AndroidSqliteDriver.Callback(schema = EmmDatabaseData.Schema) {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            },
        )
        database = EmmDatabaseData(driver)
        database.accountsQueries.insert(
            accountId = "A1",
            name = "Test Account",
            type = "Bank",
            currency = "PEN",
            updatedAt = 1_000L,
            createdAt = 1_000L,
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun orphan_fk_insert_throws_SQLiteConstraintException_the_type_applyRemoteRow_catches() {
        assertFailsWith<SQLiteConstraintException> {
            database.transactionsQueries.insertOrIgnoreFromRemote(
                transactionId = "orphan-tx",
                type = "Spend",
                amount = 100L,
                description = "",
                date = 0L,
                categoryId = null,
                accountId = "missing-acc",
                createdAt = 0L,
                updatedAt = 1_000L,
                userId = "user-1",
                deletedAt = null,
            )
        }
        assertEquals(0L, countTx("orphan-tx"), "orphan row must not be inserted")
    }

    @Test
    fun catching_fk_exception_inside_transaction_still_commits_sibling_rows() {
        database.transactionsQueries.transaction {
            database.transactionsQueries.insertOrIgnoreFromRemote(
                transactionId = "good-tx",
                type = "Spend",
                amount = 50L,
                description = "",
                date = 0L,
                categoryId = null,
                accountId = "A1",
                createdAt = 0L,
                updatedAt = 1_000L,
                userId = "user-1",
                deletedAt = null,
            )
            try {
                database.transactionsQueries.insertOrIgnoreFromRemote(
                    transactionId = "orphan-tx",
                    type = "Spend",
                    amount = 100L,
                    description = "",
                    date = 0L,
                    categoryId = null,
                    accountId = "missing-acc",
                    createdAt = 0L,
                    updatedAt = 1_000L,
                    userId = "user-1",
                    deletedAt = null,
                )
            } catch (_: SQLiteConstraintException) {
                // applyRemoteRow's skip path — swallow and keep the transaction alive.
            }
        }
        assertEquals(1L, countTx("good-tx"), "sibling row must survive the caught FK violation")
        assertEquals(0L, countTx("orphan-tx"), "orphan row must not be inserted")
    }

    private fun countTx(id: String): Long = driver.executeQuery(
        null,
        "SELECT COUNT(*) FROM transactions WHERE transactionId = '$id'",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null) },
        0,
    ).value ?: 0L
}
