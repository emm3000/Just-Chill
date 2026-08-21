package com.emm.data.loan

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class BalancesByPersonQueryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: EmmDatabaseData

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `two loans for one person and two payments on one of them are not fanned out`() {
        insertLoan(
            loanId = "loan-a",
            personName = "Ana",
            personKey = "ana",
            totalDue = 1_000L,
            lentAt = "2026-08-10T12:00:00",
        )
        insertLoan(
            loanId = "loan-b",
            personName = "Ana",
            personKey = "ana",
            totalDue = 500L,
            lentAt = "2026-08-05T12:00:00",
        )
        insertPayment(paymentId = "pay-1", loanId = "loan-a", amount = 300L)
        insertPayment(paymentId = "pay-2", loanId = "loan-a", amount = 200L)

        val row = balances().single()

        // A direct join would fan out loan-a's totalDue once per payment (1000 * 2 + 500 = 2500).
        assertEquals(1_500L, row.totalDue)
        assertEquals(500.0, row.paidSoFar)
        assertEquals(1_000L, row.totalDue!! - row.paidSoFar.toLong())
    }

    @Test
    fun `two people with different personKeys produce two rows`() {
        insertLoan(
            loanId = "loan-a",
            personName = "Ana",
            personKey = "ana",
            totalDue = 1_000L,
            lentAt = "2026-08-10T12:00:00",
        )
        insertLoan(
            loanId = "loan-b",
            personName = "Beto",
            personKey = "beto",
            totalDue = 500L,
            lentAt = "2026-08-10T12:00:00",
        )

        val rows = balances()

        assertEquals(2, rows.size)
        assertEquals(setOf("ana", "beto"), rows.map { it.personKey }.toSet())
    }

    @Test
    fun `two loans whose names fold to the same personKey collapse into one row named after the most recent loan`() {
        insertLoan(
            loanId = "loan-old",
            personName = "Ana",
            personKey = "ana",
            totalDue = 1_000L,
            lentAt = "2026-08-01T12:00:00",
        )
        insertLoan(
            loanId = "loan-new",
            personName = "Anita",
            personKey = "ana",
            totalDue = 500L,
            lentAt = "2026-08-20T12:00:00",
        )

        val row = balances().single()

        assertEquals("ana", row.personKey)
        assertEquals("Anita", row.personName)
    }

    @Test
    fun `a soft-deleted loan is excluded from the rollup`() {
        insertLoan(
            loanId = "loan-live",
            personName = "Ana",
            personKey = "ana",
            totalDue = 1_000L,
            lentAt = "2026-08-10T12:00:00",
        )
        insertLoan(
            loanId = "loan-deleted",
            personName = "Ana",
            personKey = "ana",
            totalDue = 5_000L,
            lentAt = "2026-08-15T12:00:00",
            deletedAt = 999L,
        )

        val row = balances().single()

        assertEquals(1_000L, row.totalDue)
    }

    @Test
    fun `a soft-deleted payment is excluded from the rollup`() {
        insertLoan(
            loanId = "loan-a",
            personName = "Ana",
            personKey = "ana",
            totalDue = 1_000L,
            lentAt = "2026-08-10T12:00:00",
        )
        insertPayment(paymentId = "pay-live", loanId = "loan-a", amount = 300L)
        insertPayment(paymentId = "pay-deleted", loanId = "loan-a", amount = 700L, deletedAt = 999L)

        val row = balances().single()

        assertEquals(300.0, row.paidSoFar)
    }

    private fun balances() = db.loansQueries.balancesByPerson().executeAsList()

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insertLoan(
        loanId: String,
        personName: String,
        personKey: String,
        totalDue: Long,
        lentAt: String,
        deletedAt: Long? = null,
    ) {
        exec(
            "INSERT INTO loans(loanId, personName, personKey, principal, interestBps, totalDue, note, lentAt, " +
                "createdAt, updatedAt, deletedAt) " +
                "VALUES ('$loanId', '$personName', '$personKey', $totalDue, 0, $totalDue, '', '$lentAt', 0, 0, " +
                "${deletedAt ?: "NULL"})",
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
