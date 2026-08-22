package com.emm.data.backup

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

// A pre-v4 file carries no loans, and a sweep that ran anyway would erase the ones on the device —
// the hazard ADR 009 names for `recurring_movements`, closed by the same frozen version gate.
class BackupLoanVersionGateTest {

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
        seedTwoLoansAndThreePayments()
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `restoring a version 1 file leaves every local loan and payment untouched`() = runTest {
        assertLoanLedgerSurvives(V1_BACKUP)
    }

    @Test
    fun `restoring a version 2 file leaves every local loan and payment untouched`() = runTest {
        assertLoanLedgerSurvives(V2_BACKUP)
    }

    @Test
    fun `restoring a version 3 file leaves every local loan and payment untouched`() = runTest {
        assertLoanLedgerSurvives(V3_BACKUP)
    }

    private suspend fun assertLoanLedgerSurvives(json: String) {
        val stats = repository.importFromJson(json)

        assertEquals(0, stats.loans, "the file carries no loans, so none were restored")
        assertEquals(0, stats.loanPayments, "the file carries no payments, so none were restored")
        assertEquals(2, rawCount("SELECT COUNT(*) FROM loans WHERE deletedAt IS NULL"))
        assertEquals(3, rawCount("SELECT COUNT(*) FROM loan_payments WHERE deletedAt IS NULL"))
        assertEquals(
            2,
            rawCount("SELECT COUNT(*) FROM loans WHERE updatedAt = $SEEDED_AT"),
            "a loan row was written by an import that cannot carry one",
        )
        assertEquals(
            3,
            rawCount("SELECT COUNT(*) FROM loan_payments WHERE updatedAt = $SEEDED_AT"),
            "a payment row was written by an import that cannot carry one",
        )
        assertEquals(1_000_00L, db.loansQueries.byId("loan-1").executeAsOne().totalDue)
    }

    private fun seedTwoLoansAndThreePayments() {
        insertLoan(loanId = "loan-1", personName = "Andrés", totalDue = 1_000_00L)
        insertLoan(loanId = "loan-2", personName = "Rosa", totalDue = 200_00L)
        insertPayment(paymentId = "pay-1", loanId = "loan-1", amount = 300_00L)
        insertPayment(paymentId = "pay-2", loanId = "loan-1", amount = 100_00L)
        insertPayment(paymentId = "pay-3", loanId = "loan-2", amount = 50_00L)
    }

    private fun insertLoan(loanId: String, personName: String, totalDue: Long) {
        db.loansQueries.insert(
            loanId = loanId,
            personName = personName,
            personKey = personName.lowercase(),
            principal = totalDue,
            interestBps = 0L,
            totalDue = totalDue,
            note = "",
            lentAt = "2026-07-01T10:00:00",
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
        )
    }

    private fun insertPayment(paymentId: String, loanId: String, amount: Long) {
        db.loan_paymentsQueries.insert(
            paymentId = paymentId,
            loanId = loanId,
            amount = amount,
            method = "Cash",
            paidAt = "2026-07-20T10:00:00",
            note = "",
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
        )
    }

    private fun rawCount(sql: String): Long = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 0,
    ).value

    private companion object {

        const val SEEDED_AT = 1_000L
        val IMPORTED_AT: Instant = Instant.parse("2026-08-11T15:04:05Z")

        const val ACCOUNT_JSON = """{"accountId":"acc-1","name":"BCP","type":"Bank","currency":"PEN"}"""

        const val V1_BACKUP = """
{
    "schemaVersion": 1,
    "exportedAt": 1745000000000,
    "appVersion": "2.0.0",
    "accounts": [$ACCOUNT_JSON],
    "categories": [],
    "transactions": []
}
"""

        const val V2_BACKUP = """
{
    "schemaVersion": 2,
    "exportedAt": 1755000000000,
    "appVersion": "2.4.0",
    "accounts": [$ACCOUNT_JSON],
    "categories": [],
    "transactions": []
}
"""

        const val V3_BACKUP = """
{
    "schemaVersion": 3,
    "exportedAt": 1765000000000,
    "appVersion": "2.5.0",
    "accounts": [$ACCOUNT_JSON],
    "categories": [],
    "transactions": [],
    "recurringMovements": []
}
"""
    }
}
