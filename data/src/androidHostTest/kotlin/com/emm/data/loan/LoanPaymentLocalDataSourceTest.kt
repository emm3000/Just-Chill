package com.emm.data.loan

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.domain.loan.LoanPayment
import com.emm.domain.loan.PaymentMethod
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.LoanPaymentId
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

class LoanPaymentLocalDataSourceTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var localDataSource: LoanPaymentLocalDataSource

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-21T09:00:00Z")
    }

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        localDataSource = LoanPaymentLocalDataSource(db, clock)
        db.loansQueries.insert(
            loanId = "loan-1",
            personName = "Ana",
            personKey = "ana",
            principal = 100_000L,
            interestBps = 0L,
            totalDue = 100_000L,
            note = "",
            lentAt = "2026-08-01T09:00:00",
            createdAt = 0L,
            updatedAt = 0L,
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `create round-trips every field through byId`() = runTest {
        val payment = payment()

        localDataSource.create(payment)

        val result = localDataSource.byId(payment.id.value).first()

        assertNotNull(result)
        assertPaymentFields(payment, result)
    }

    @Test
    fun `update changes amount, method, paidAt and note, and byId reflects it`() = runTest {
        localDataSource.create(payment())

        val edited = payment(
            amount = 5_000L,
            method = PaymentMethod.Transfer,
            paidAt = "2026-08-15T09:30:00",
            note = "Corregido",
        )
        localDataSource.update(edited)

        val result = localDataSource.byId(edited.id.value).first()

        assertNotNull(result)
        assertPaymentFields(edited, result)
    }

    @Test
    fun `update bumps updatedAt and sets syncState to Pending, even starting from Synced`() = runTest {
        // Inserted by raw SQL, not the generated `insert:` query, because that query hardcodes
        // syncState = 'Pending' — a prior sync is the only way to start this row as 'Synced'.
        driver.execute(
            identifier = null,
            sql = "INSERT INTO loan_payments(paymentId, loanId, amount, method, paidAt, note, " +
                "createdAt, updatedAt, syncState) VALUES ('pay-1', 'loan-1', 300, 'Cash', " +
                "'2026-08-11T12:00:00', '', 100, 100, 'Synced')",
            parameters = 0,
        )

        localDataSource.update(payment(amount = 5_000L))

        val row = db.loan_paymentsQueries.all().executeAsList().single { it.paymentId == "pay-1" }
        assertEquals("Pending", row.syncState)
        assertEquals(clock.now().toEpochMilliseconds(), row.updatedAt)
    }

    @Test
    fun `update does not touch a soft-deleted payment`() = runTest {
        localDataSource.create(payment(amount = 300L))
        localDataSource.softDelete("pay-1")

        localDataSource.update(payment(amount = 999_00L))

        // `all()` filters `deletedAt IS NULL`, so a tombstoned row never comes back through it —
        // read the column directly to prove `update:`'s own `AND deletedAt IS NULL` blocked the write.
        assertEquals(300L, rawAmount("pay-1"))
    }

    private fun rawAmount(paymentId: String): Long? = driver.executeQuery(
        identifier = null,
        sql = "SELECT amount FROM loan_payments WHERE paymentId = '$paymentId'",
        mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null) },
        parameters = 0,
    ).value

    private fun assertPaymentFields(expected: LoanPayment, actual: LoanPayment) {
        assertEquals(expected.loanId, actual.loanId)
        assertEquals(expected.amount, actual.amount)
        assertEquals(expected.method, actual.method)
        assertEquals(expected.paidAt, actual.paidAt)
        assertEquals(expected.note, actual.note)
    }

    private fun payment(
        paymentId: String = "pay-1",
        loanId: String = "loan-1",
        amount: Long = 300L,
        method: PaymentMethod = PaymentMethod.Cash,
        paidAt: String = "2026-08-11T12:00:00",
        note: String = "",
    ) = LoanPayment(
        id = LoanPaymentId(paymentId),
        loanId = LoanId(loanId),
        amount = Money(amount),
        method = method,
        paidAt = LocalDateTime.parse(paidAt),
        note = note,
    )
}
