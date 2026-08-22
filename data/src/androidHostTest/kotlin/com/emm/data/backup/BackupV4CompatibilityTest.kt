package com.emm.data.backup

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.domain.shared.backup.ImportStats
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

class BackupV4CompatibilityTest {

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
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `the current shape parses the bytes a version 4 export writes`() {
        val payload = importJson.decodeFromString<ExportPayloadDto>(V4_BACKUP)

        assertEquals(BACKUP_SCHEMA_VERSION, payload.schemaVersion)
        assertEquals(listOf("loan-1", "loan-2"), payload.loans.map { it.loanId })
        assertEquals(listOf("pay-1", "pay-2"), payload.loanPayments.map { it.paymentId })
    }

    @Test
    fun `a version 4 file reaches the import declaring version 4`() {
        val decoded = decodeBackupPayload(V4_BACKUP)

        assertEquals(BACKUP_SCHEMA_VERSION, decoded.declaredVersion)
        assertEquals(BACKUP_LOANS_SINCE_VERSION, decoded.declaredVersion)
    }

    @Test
    fun `a version 4 file with no loans key is refused as corrupt, not read as none`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            repository.importFromJson(V4_BACKUP_WITHOUT_LOANS)
        }

        assertEquals(ValidationCode.BackupFileInvalid, ex.code)
    }

    @Test
    fun `a version 4 file restores the loans and the payments it carries`() = runTest {
        val stats = repository.importFromJson(V4_BACKUP)

        assertEquals(
            ImportStats(
                accounts = 1,
                categories = 0,
                transactions = 0,
                recurring = 0,
                loans = 2,
                loanPayments = 2,
            ),
            stats,
        )
        assertEquals(2, rawCount("SELECT COUNT(*) FROM loans WHERE deletedAt IS NULL"))
        assertEquals(2, rawCount("SELECT COUNT(*) FROM loan_payments WHERE deletedAt IS NULL"))
    }

    @Test
    fun `every loan field the version 4 file carries lands in the row`() = runTest {
        repository.importFromJson(V4_BACKUP)

        val loan = db.loansQueries.byId("loan-1").executeAsOne()
        assertEquals("Andrés Muñoz", loan.personName)
        assertEquals("andres munoz", loan.personKey)
        assertEquals(50_000L, loan.principal)
        assertEquals(500L, loan.interestBps)
        assertEquals(52_500L, loan.totalDue)
        assertEquals("Para la mudanza", loan.note)
        assertEquals("2026-08-01T09:00:00", loan.lentAt)
    }

    @Test
    fun `every payment field the version 4 file carries lands in the row`() = runTest {
        repository.importFromJson(V4_BACKUP)

        val payment = db.loan_paymentsQueries.byLoan("loan-1").executeAsOne()
        assertEquals("pay-1", payment.paymentId)
        assertEquals(20_000L, payment.amount)
        assertEquals("Transfer", payment.method)
        assertEquals("2026-08-12T18:30:00", payment.paidAt)
        assertEquals("Primer abono", payment.note)
    }

    @Test
    fun `a restored loan comes back unclaimed and Pending, stamped by the importing clock`() = runTest {
        repository.importFromJson(V4_BACKUP)

        val loan = db.loansQueries.byId("loan-1").executeAsOne()
        assertEquals(null, loan.userId)
        assertEquals("Pending", loan.syncState)
        assertEquals(IMPORT_STAMP, loan.createdAt)
        assertEquals(IMPORT_STAMP, loan.updatedAt)
    }

    @Test
    fun `a payment the app cannot read is dropped and the reported count says so`() = runTest {
        val stats = repository.importFromJson(V4_BACKUP_WITH_UNREADABLE_PAYMENTS)

        assertEquals(1, stats.loans)
        assertEquals(1, stats.loanPayments)
        assertEquals(
            listOf("pay-good"),
            db.loan_paymentsQueries.byLoan("loan-1").executeAsList().map { it.paymentId },
        )
    }

    @Test
    fun `a payment whose loan the file never carried is dropped, not left to abort the restore`() = runTest {
        val stats = repository.importFromJson(V4_BACKUP_WITH_ORPHAN_PAYMENT)

        assertEquals(1, stats.loans)
        assertEquals(0, stats.loanPayments)
        assertEquals(0, rawCount("SELECT COUNT(*) FROM loan_payments"))
    }

    @Test
    fun `a version 4 file tombstones the loans the device holds and leaves only its own live`() = runTest {
        seedLocalLoan(loanId = "loan-on-device", paymentId = "pay-on-device")

        repository.importFromJson(V4_BACKUP)

        assertEquals(3, rawCount("SELECT COUNT(*) FROM loans"), "a row was deleted outright")
        assertEquals(
            listOf("loan-1", "loan-2"),
            db.loansQueries.all().executeAsList().map { it.loanId }.sorted(),
        )
        assertEquals(
            listOf("pay-1", "pay-2"),
            db.loan_paymentsQueries.all().executeAsList().map { it.paymentId }.sorted(),
        )
        assertEquals(
            1,
            rawCount("SELECT COUNT(*) FROM loans WHERE loanId = 'loan-on-device' AND deletedAt IS NOT NULL"),
        )
    }

    private fun seedLocalLoan(loanId: String, paymentId: String) {
        db.accountsQueries.insert(
            accountId = "acc-local",
            name = "Yape",
            type = "Wallet",
            currency = "PEN",
            updatedAt = SEEDED_AT,
            createdAt = SEEDED_AT,
        )
        db.loansQueries.insert(
            loanId = loanId,
            personName = "Rosa",
            personKey = "rosa",
            principal = 10_000L,
            interestBps = 0L,
            totalDue = 10_000L,
            note = "",
            lentAt = "2026-07-01T10:00:00",
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
        )
        db.loan_paymentsQueries.insert(
            paymentId = paymentId,
            loanId = loanId,
            amount = 5_000L,
            method = "Cash",
            paidAt = "2026-07-10T10:00:00",
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

        val importJson = Json { ignoreUnknownKeys = true }

        const val SEEDED_AT = 1_000L
        val IMPORTED_AT: Instant = Instant.parse("2026-08-11T15:04:05Z")
        val IMPORT_STAMP: Long = IMPORTED_AT.toEpochMilliseconds()

        const val V4_BACKUP = """
{
    "schemaVersion": 4,
    "exportedAt": 1785000000000,
    "appVersion": "2.6.0",
    "accounts": [
        { "accountId": "acc-1", "name": "BCP", "type": "Bank", "currency": "PEN" }
    ],
    "categories": [],
    "transactions": [],
    "recurringMovements": [],
    "loans": [
        {
            "loanId": "loan-1",
            "personName": "Andrés Muñoz",
            "personKey": "andres munoz",
            "principalCents": 50000,
            "interestBps": 500,
            "totalDueCents": 52500,
            "note": "Para la mudanza",
            "lentAt": "2026-08-01T09:00:00"
        },
        {
            "loanId": "loan-2",
            "personName": "Rosa",
            "personKey": "rosa",
            "principalCents": 8000,
            "interestBps": 0,
            "totalDueCents": 8000,
            "note": "",
            "lentAt": "2026-08-05T20:15:00"
        }
    ],
    "loanPayments": [
        {
            "paymentId": "pay-1",
            "loanId": "loan-1",
            "amountCents": 20000,
            "method": "Transfer",
            "paidAt": "2026-08-12T18:30:00",
            "note": "Primer abono"
        },
        {
            "paymentId": "pay-2",
            "loanId": "loan-2",
            "amountCents": 3000,
            "method": "Cash",
            "paidAt": "2026-08-13T08:00:00",
            "note": ""
        }
    ]
}
"""

        const val V4_BACKUP_WITH_UNREADABLE_PAYMENTS = """
{
    "schemaVersion": 4,
    "exportedAt": 1785000000000,
    "appVersion": "2.6.0",
    "accounts": [],
    "categories": [],
    "transactions": [],
    "recurringMovements": [],
    "loans": [
        {
            "loanId": "loan-1",
            "personName": "Andrés Muñoz",
            "personKey": "andres munoz",
            "principalCents": 50000,
            "interestBps": 500,
            "totalDueCents": 52500,
            "note": "Para la mudanza",
            "lentAt": "2026-08-01T09:00:00"
        }
    ],
    "loanPayments": [
        {
            "paymentId": "pay-good",
            "loanId": "loan-1",
            "amountCents": 20000,
            "method": "Cash",
            "paidAt": "2026-08-12T18:30:00",
            "note": ""
        },
        {
            "paymentId": "pay-bad-method",
            "loanId": "loan-1",
            "amountCents": 1000,
            "method": "Bitcoin",
            "paidAt": "2026-08-12T18:30:00",
            "note": ""
        },
        {
            "paymentId": "pay-bad-date",
            "loanId": "loan-1",
            "amountCents": 1000,
            "method": "Cash",
            "paidAt": "2026-8-1 12:00",
            "note": ""
        }
    ]
}
"""

        const val V4_BACKUP_WITH_ORPHAN_PAYMENT = """
{
    "schemaVersion": 4,
    "exportedAt": 1785000000000,
    "appVersion": "2.6.0",
    "accounts": [],
    "categories": [],
    "transactions": [],
    "recurringMovements": [],
    "loans": [
        {
            "loanId": "loan-1",
            "personName": "Rosa",
            "personKey": "rosa",
            "principalCents": 8000,
            "interestBps": 0,
            "totalDueCents": 8000,
            "note": "",
            "lentAt": "2026-08-05T20:15:00"
        }
    ],
    "loanPayments": [
        {
            "paymentId": "pay-orphan",
            "loanId": "loan-the-file-does-not-carry",
            "amountCents": 1000,
            "method": "Cash",
            "paidAt": "2026-08-12T18:30:00",
            "note": ""
        }
    ]
}
"""

        const val V4_BACKUP_WITHOUT_LOANS = """
{
    "schemaVersion": 4,
    "exportedAt": 1785000000000,
    "appVersion": "2.6.0",
    "accounts": [],
    "categories": [],
    "transactions": [],
    "recurringMovements": [],
    "loanPayments": []
}
"""
    }
}
