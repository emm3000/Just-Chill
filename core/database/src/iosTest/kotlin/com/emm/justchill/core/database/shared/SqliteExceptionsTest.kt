package com.emm.justchill.core.database.shared

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.sqliter.DatabaseFileContext
import com.emm.justchill.core.database.databaseConfiguration
import com.emm.justchill.core.database.openSqlDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val INSERT_DUPLICATE_SEEDED_CATEGORY: String =
    "INSERT INTO categories (categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) " +
        "VALUES ('c3c1d0a2-8f12-4b9e-9a36-1c4d2f0b2f01', 'Duplicado', 'groceries', 'green', 'Spend', 0, 0, 0)"

private const val INSERT_PAYMENT_FOR_MISSING_LOAN: String =
    "INSERT INTO loan_payments (paymentId, loanId, amount, method, paidAt, createdAt, updatedAt) " +
        "VALUES ('payment', 'missing-loan', 100, 'Cash', '2026-09-29', 0, 0)"

class SqliteExceptionsTest {

    private val databaseName: String = "sqlite-exceptions-test.db"
    private lateinit var driver: SqlDriver

    @BeforeTest
    fun setUp() {
        DatabaseFileContext.deleteDatabase(databaseName)
        driver = openSqlDriver(databaseConfiguration(databaseName))
    }

    @AfterTest
    fun tearDown() {
        driver.close()
        DatabaseFileContext.deleteDatabase(databaseName)
    }

    @Test
    fun `a duplicate seeded category id is a constraint violation`() {
        val failure: Throwable = assertFailsWith<Throwable> {
            driver.execute(identifier = null, sql = INSERT_DUPLICATE_SEEDED_CATEGORY, parameters = 0)
        }

        assertTrue(failure.isSqliteConstraintViolation())
    }

    @Test
    fun `a payment for a missing loan is a constraint violation`() {
        val failure: Throwable = assertFailsWith<Throwable> {
            driver.execute(identifier = null, sql = INSERT_PAYMENT_FOR_MISSING_LOAN, parameters = 0)
        }

        assertTrue(failure.isSqliteConstraintViolation())
    }

    @Test
    fun `a query on a missing table is a SQLite error but not a constraint violation`() {
        val failure: Throwable = assertFailsWith<Throwable> {
            driver.execute(identifier = null, sql = "DELETE FROM missing_table", parameters = 0)
        }

        assertTrue(failure.isSqliteException())
        assertFalse(failure.isSqliteConstraintViolation())
    }
}
