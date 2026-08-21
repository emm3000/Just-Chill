package com.emm.data.loan

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class PaidSoFarQueryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var localDataSource: LoanPaymentLocalDataSource

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-11T15:04:05Z")
    }

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        localDataSource = LoanPaymentLocalDataSource(db, clock)
        exec(
            "INSERT INTO loans(loanId, personName, personKey, principal, interestBps, totalDue, note, lentAt, " +
                "createdAt, updatedAt) " +
                "VALUES ('loan-1', 'Ana', 'ana', 1000, 0, 1000, '', '2026-08-10T12:00:00', 0, 0)",
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `paidSoFar for a loan with no payments returns Money Zero, not null`() = runTest {
        val result = localDataSource.paidSoFar(LoanId("loan-1").value)

        assertEquals(Money.Zero, result)
    }

    @Test
    fun `paidSoFar sums only the live payments for that loan`() = runTest {
        insertLoan(loanId = "loan-2")
        insertPayment(paymentId = "pay-live", loanId = "loan-1", amount = 300L)
        insertPayment(paymentId = "pay-deleted", loanId = "loan-1", amount = 700L, deletedAt = 999L)
        insertPayment(paymentId = "pay-other-loan", loanId = "loan-2", amount = 900L)

        val result = localDataSource.paidSoFar(LoanId("loan-1").value)

        assertEquals(Money(300L), result)
    }

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insertLoan(loanId: String) {
        exec(
            "INSERT INTO loans(loanId, personName, personKey, principal, interestBps, totalDue, note, lentAt, " +
                "createdAt, updatedAt) " +
                "VALUES ('$loanId', 'Beto', 'beto', 500, 0, 500, '', '2026-08-10T12:00:00', 0, 0)",
        )
    }

    private fun insertPayment(paymentId: String, loanId: String, amount: Long, deletedAt: Long? = null) {
        exec(
            "INSERT INTO loan_payments(paymentId, loanId, amount, method, paidAt, note, createdAt, updatedAt, " +
                "deletedAt) " +
                "VALUES ('$paymentId', '$loanId', $amount, 'Cash', '2026-08-11T12:00:00', '', 0, 0, " +
                "${deletedAt ?: "NULL"})",
        )
    }
}
