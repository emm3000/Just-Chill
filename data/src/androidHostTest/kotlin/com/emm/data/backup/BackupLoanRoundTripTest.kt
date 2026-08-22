package com.emm.data.backup

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.data.Loan_payments
import com.emm.data.Loans
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class BackupLoanRoundTripTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var repository: DefaultBackupRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        db = EmmDatabaseData(driver)
        repository = DefaultBackupRepository(
            db = db,
            clock = object : Clock {
                override fun now(): Instant = IMPORTED_AT
            },
        )
        seedFixture()
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `the file carries every live loan and payment and no tombstoned one`() = runTest {
        val payload = exportedPayload()

        assertEquals(listOf("loan-interest", "loan-plain"), payload.loans.map { it.loanId }.sorted())
        assertEquals(listOf("pay-cash", "pay-transfer"), payload.loanPayments.map { it.paymentId }.sorted())
    }

    @Test
    fun `a payment whose loan is gone is dropped from the file rather than exported orphaned`() = runTest {
        db.loansQueries.softDelete(deletedAt = SEEDED_AT, updatedAt = SEEDED_AT, loanId = "loan-plain")

        val payload = exportedPayload()

        assertEquals(listOf("loan-interest"), payload.loans.map { it.loanId })
        assertEquals(listOf("pay-transfer"), payload.loanPayments.map { it.paymentId })
    }

    @Test
    fun `a loan survives every field it was seeded with`() = runTest {
        roundTrip()

        val loan = loan("loan-interest")
        assertEquals("Andrés Muñoz", loan.personName)
        assertEquals("andres munoz", loan.personKey)
        assertEquals(1_500_00L, loan.principal)
        assertEquals(750L, loan.interestBps)
        assertEquals(1_612_50L, loan.totalDue)
        assertEquals("Para la mudanza", loan.note)
        assertEquals("2026-08-01T09:00:00", loan.lentAt)
    }

    @Test
    fun `a loan with no interest and no note survives as such`() = runTest {
        roundTrip()

        val loan = loan("loan-plain")
        assertEquals("Rosa", loan.personName)
        assertEquals("rosa", loan.personKey)
        assertEquals(80_00L, loan.principal)
        assertEquals(0L, loan.interestBps)
        assertEquals(80_00L, loan.totalDue)
        assertEquals("", loan.note)
        assertEquals("2026-08-05T20:15:00", loan.lentAt)
    }

    @Test
    fun `a payment survives amount, method, paidAt and note`() = runTest {
        roundTrip()

        val transfer = payment("pay-transfer")
        assertEquals("loan-interest", transfer.loanId)
        assertEquals(500_00L, transfer.amount)
        assertEquals("Transfer", transfer.method)
        assertEquals("2026-08-12T18:30:00", transfer.paidAt)
        assertEquals("Primer abono", transfer.note)
        val cash = payment("pay-cash")
        assertEquals("loan-plain", cash.loanId)
        assertEquals(30_00L, cash.amount)
        assertEquals("Cash", cash.method)
        assertEquals("2026-08-13T08:00:00", cash.paidAt)
        assertEquals("", cash.note)
    }

    @Test
    fun `a tombstoned loan and its payment do not come back`() = runTest {
        roundTrip()

        assertTrue(db.loansQueries.byId("loan-dead").executeAsOneOrNull() == null)
        assertTrue(db.loan_paymentsQueries.byLoan("loan-dead").executeAsList().isEmpty())
    }

    @Test
    fun `a restored loan and payment come back unclaimed and Pending`() = runTest {
        roundTrip()

        assertNull(loan("loan-interest").userId)
        assertEquals("Pending", loan("loan-interest").syncState)
        assertNull(payment("pay-transfer").userId)
        assertEquals("Pending", payment("pay-transfer").syncState)
    }

    private suspend fun roundTrip() {
        val json = repository.exportToJson(exportedAt = EXPORTED_AT, appVersion = APP_VERSION)
        wipePhysically()
        repository.importFromJson(json)
    }

    private suspend fun exportedPayload(): ExportPayloadDto =
        Json.decodeFromString(repository.exportToJson(exportedAt = EXPORTED_AT, appVersion = APP_VERSION))

    private fun wipePhysically() {
        driver.execute(null, "DELETE FROM loan_payments", 0)
        driver.execute(null, "DELETE FROM loans", 0)
    }

    private fun loan(loanId: String): Loans = db.loansQueries.byId(loanId).executeAsOne()

    private fun payment(paymentId: String): Loan_payments = db.loan_paymentsQueries.all()
        .executeAsList()
        .single { it.paymentId == paymentId }

    private fun seedFixture() {
        insertLoan(
            loanId = "loan-interest",
            personName = "Andrés Muñoz",
            personKey = "andres munoz",
            principal = 1_500_00L,
            interestBps = 750L,
            totalDue = 1_612_50L,
            note = "Para la mudanza",
            lentAt = "2026-08-01T09:00:00",
        )
        insertLoan(
            loanId = "loan-plain",
            personName = "Rosa",
            personKey = "rosa",
            principal = 80_00L,
            interestBps = 0L,
            totalDue = 80_00L,
            note = "",
            lentAt = "2026-08-05T20:15:00",
        )
        insertLoan(
            loanId = "loan-dead",
            personName = "Cobrado",
            personKey = "cobrado",
            principal = 10_00L,
            interestBps = 0L,
            totalDue = 10_00L,
            note = "",
            lentAt = "2026-06-01T09:00:00",
        )
        insertPayment("pay-transfer", "loan-interest", 500_00L, "Transfer", "2026-08-12T18:30:00", "Primer abono")
        insertPayment("pay-cash", "loan-plain", 30_00L, "Cash", "2026-08-13T08:00:00", "")
        insertPayment("pay-dead", "loan-dead", 10_00L, "Cash", "2026-06-10T08:00:00", "")
        db.loansQueries.softDelete(deletedAt = SEEDED_AT, updatedAt = SEEDED_AT, loanId = "loan-dead")
        db.loan_paymentsQueries.softDeleteByLoan(deletedAt = SEEDED_AT, updatedAt = SEEDED_AT, loanId = "loan-dead")
    }

    private fun insertLoan(
        loanId: String,
        personName: String,
        personKey: String,
        principal: Long,
        interestBps: Long,
        totalDue: Long,
        note: String,
        lentAt: String,
    ) {
        db.loansQueries.insert(
            loanId = loanId,
            personName = personName,
            personKey = personKey,
            principal = principal,
            interestBps = interestBps,
            totalDue = totalDue,
            note = note,
            lentAt = lentAt,
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
        )
    }

    private fun insertPayment(
        paymentId: String,
        loanId: String,
        amount: Long,
        method: String,
        paidAt: String,
        note: String,
    ) {
        db.loan_paymentsQueries.insert(
            paymentId = paymentId,
            loanId = loanId,
            amount = amount,
            method = method,
            paidAt = paidAt,
            note = note,
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
        )
    }

    private companion object {
        const val APP_VERSION = "2.6.0"
        const val SEEDED_AT = 1_000L
        val EXPORTED_AT: Long = Instant.parse("2026-08-16T10:00:00Z").toEpochMilliseconds()
        val IMPORTED_AT: Instant = Instant.parse("2026-08-16T11:30:00Z")
    }
}
