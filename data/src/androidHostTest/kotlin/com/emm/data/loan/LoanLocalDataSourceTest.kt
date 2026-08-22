package com.emm.data.loan

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.domain.loan.Loan
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.Instant

class LoanLocalDataSourceTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var localDataSource: LoanLocalDataSource

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-21T09:00:00Z")
    }

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        localDataSource = LoanLocalDataSource(db, clock)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `create round-trips every field through byId and loansWithBalance`() = runTest {
        val loan = loan()

        localDataSource.create(loan)

        val byIdResult = localDataSource.byId(loan.id.value).first()
        val loansWithBalanceResult = localDataSource.loansWithBalance(loan.personKey).first().single()

        assertNotNull(byIdResult)
        assertLoanFields(loan, byIdResult)
        assertLoanFields(loan, loansWithBalanceResult.loan)
    }

    @Test
    fun `update changes the fields it should and byId reflects it`() = runTest {
        localDataSource.create(loan())

        val updated = loan(
            personName = "Beto",
            personKey = "beto",
            principal = 200_000L,
            interestBps = 500,
            totalDue = 210_000L,
            note = "Pagará en dos partes",
            lentAt = "2026-08-15T09:30:00",
        )
        localDataSource.update(updated)

        val result = localDataSource.byId(updated.id.value).first()

        assertNotNull(result)
        assertLoanFields(updated, result)
    }

    @Test
    fun `balancesByPerson through the data source returns the expected PersonBalance`() = runTest {
        localDataSource.create(loan(loanId = "loan-1", totalDue = 10_000L))
        insertPayment(paymentId = "pay-1", loanId = "loan-1", amount = 1_000L)
        insertPayment(paymentId = "pay-2", loanId = "loan-1", amount = 2_000L)

        val result = localDataSource.balancesByPerson().first().single()

        assertEquals("ana", result.personKey)
        assertEquals("Ana", result.personName)
        assertEquals(Money(7_000L), result.remaining)
    }

    @Test
    fun `loansWithBalance excludes a soft-deleted loan`() = runTest {
        localDataSource.create(loan(loanId = "loan-live", lentAt = "2026-08-10T12:00:00"))
        localDataSource.create(loan(loanId = "loan-deleted", lentAt = "2026-08-15T12:00:00"))

        localDataSource.softDelete("loan-deleted")

        val result = localDataSource.loansWithBalance("ana").first()

        assertEquals(1, result.size)
        assertEquals("loan-live", result.single().loan.id.value)
    }

    @Test
    fun `loansWithBalance reports paidSoFar and remaining per loan, excluding a soft-deleted payment`() = runTest {
        localDataSource.create(loan(loanId = "loan-1", totalDue = 10_000L, lentAt = "2026-08-10T12:00:00"))
        localDataSource.create(loan(loanId = "loan-2", totalDue = 5_000L, lentAt = "2026-08-12T12:00:00"))
        insertPayment(paymentId = "pay-1", loanId = "loan-1", amount = 1_000L)
        insertPayment(paymentId = "pay-2", loanId = "loan-1", amount = 2_000L)
        insertPayment(paymentId = "pay-deleted", loanId = "loan-1", amount = 5_000L, deletedAt = 999L)

        val result = localDataSource.loansWithBalance("ana").first()

        val loanOne = result.single { it.loan.id.value == "loan-1" }
        val loanTwo = result.single { it.loan.id.value == "loan-2" }
        assertEquals(Money(3_000L), loanOne.paidSoFar)
        assertEquals(Money(7_000L), loanOne.remaining)
        assertEquals(Money(0L), loanTwo.paidSoFar)
        assertEquals(Money(5_000L), loanTwo.remaining)
    }

    private fun assertLoanFields(expected: Loan, actual: Loan) {
        assertEquals(expected.personName, actual.personName)
        assertEquals(expected.personKey, actual.personKey)
        assertEquals(expected.principal, actual.principal)
        assertEquals(expected.interestBps, actual.interestBps)
        assertEquals(expected.totalDue, actual.totalDue)
        assertEquals(expected.note, actual.note)
        assertEquals(expected.lentAt, actual.lentAt)
    }

    private fun loan(
        loanId: String = "loan-1",
        personName: String = "Ana",
        personKey: String = "ana",
        principal: Long = 100_000L,
        interestBps: Int = 250,
        totalDue: Long = 102_500L,
        note: String = "Prestamo para el alquiler",
        lentAt: String = "2026-08-10T12:00:00",
    ) = Loan(
        id = LoanId(loanId),
        personName = personName,
        personKey = personKey,
        principal = Money(principal),
        interestBps = interestBps,
        totalDue = Money(totalDue),
        note = note,
        lentAt = LocalDateTime.parse(lentAt),
    )

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
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
