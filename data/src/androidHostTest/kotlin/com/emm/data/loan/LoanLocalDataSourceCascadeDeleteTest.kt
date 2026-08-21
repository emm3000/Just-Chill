package com.emm.data.loan

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class LoanLocalDataSourceCascadeDeleteTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var localDataSource: LoanLocalDataSource

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-21T09:00:00Z")
    }

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        localDataSource = LoanLocalDataSource(db, clock)
        exec(
            "INSERT INTO loans(loanId, personName, personKey, principal, interestBps, totalDue, note, lentAt, " +
                "createdAt, updatedAt) " +
                "VALUES ('loan-1', 'Ana', 'ana', 1000, 0, 1000, '', '2026-08-10T12:00:00', 0, 0)",
        )
        insertPayment(paymentId = "pay-1", loanId = "loan-1", amount = 300L)
        insertPayment(paymentId = "pay-2", loanId = "loan-1", amount = 200L)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `softDelete tombstones the loan and every live payment under it in the same call`() = runTest {
        localDataSource.softDelete("loan-1")

        val liveLoanRow = db.loansQueries.byId("loan-1").executeAsOneOrNull()

        assertNull(liveLoanRow, "byId filters deletedAt IS NULL, so a soft-deleted loan is absent")
        assertNotNull(rawLoanDeletedAt(), "the loan row itself must carry a deletedAt stamp")
        assertTrue(db.loan_paymentsQueries.byLoan("loan-1").executeAsList().isEmpty())
        assertEquals(0L, liveLoanPaymentCount())
    }

    private fun rawLoanDeletedAt(): Long? = driver.executeQuery(
        null,
        "SELECT deletedAt FROM loans WHERE loanId = 'loan-1'",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null) },
        0,
    ).value

    private fun liveLoanPaymentCount(): Long = driver.executeQuery(
        null,
        "SELECT COUNT(*) FROM loan_payments WHERE loanId = 'loan-1' AND deletedAt IS NULL",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null) },
        0,
    ).value ?: 0L

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insertPayment(paymentId: String, loanId: String, amount: Long) {
        exec(
            "INSERT INTO loan_payments(paymentId, loanId, amount, method, paidAt, note, createdAt, updatedAt) " +
                "VALUES ('$paymentId', '$loanId', $amount, 'Cash', '2026-08-11T12:00:00', '', 0, 0)",
        )
    }
}
