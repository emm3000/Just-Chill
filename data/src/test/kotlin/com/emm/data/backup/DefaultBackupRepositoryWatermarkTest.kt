package com.emm.data.backup

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultBackupRepositoryWatermarkTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var repository: DefaultBackupRepository

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-14T00:00:00Z")
    }

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        db = EmmDatabaseData(driver)
        repository = DefaultBackupRepository(db = db, clock = clock)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `empty database returns null`() = runTest {
        assertNull(repository.latestLocalChangeAt())
    }

    @Test
    fun `a row in accounts alone is the watermark`() = runTest {
        insertAccount(updatedAt = 111L)

        assertEquals(111L, repository.latestLocalChangeAt())
    }

    @Test
    fun `a row in categories alone is the watermark`() = runTest {
        insertCategory(updatedAt = 222L)

        assertEquals(222L, repository.latestLocalChangeAt())
    }

    @Test
    fun `a row in transactions alone is the watermark`() = runTest {
        insertAccount(updatedAt = 1L)
        insertTransaction(updatedAt = 333L)

        assertEquals(333L, repository.latestLocalChangeAt())
    }

    @Test
    fun `a row in recurring_movements alone is the watermark`() = runTest {
        insertAccount(updatedAt = 1L)
        insertTemplate(updatedAt = 444L)

        assertEquals(444L, repository.latestLocalChangeAt())
    }

    @Test
    fun `a row in loans alone is the watermark`() = runTest {
        insertLoan(updatedAt = 555L)

        assertEquals(555L, repository.latestLocalChangeAt())
    }

    @Test
    fun `a row in loan_payments alone is the watermark`() = runTest {
        insertLoan(updatedAt = 1L)
        insertLoanPayment(updatedAt = 666L)

        assertEquals(666L, repository.latestLocalChangeAt())
    }

    @Test
    fun `the watermark is the maximum across tables, wherever it lives`() = runTest {
        insertAccount(updatedAt = 100L)
        insertCategory(updatedAt = 500L)
        insertTransaction(updatedAt = 200L)
        insertTemplate(updatedAt = 300L)

        assertEquals(500L, repository.latestLocalChangeAt())
    }

    @Test
    fun `the maximum living in the last UNION ALL branch is still found`() = runTest {
        insertAccount(updatedAt = 100L)
        insertCategory(updatedAt = 200L)
        insertTransaction(updatedAt = 300L)
        insertTemplate(updatedAt = 400L)
        insertLoan(updatedAt = 500L)
        insertLoanPayment(updatedAt = 999L)

        assertEquals(999L, repository.latestLocalChangeAt())
    }

    @Test
    fun `a soft-deleted row is counted, not excluded`() = runTest {
        insertAccount(accountId = "acc-1", updatedAt = 1L)

        db.accountsQueries.softDelete(deletedAt = 999L, updatedAt = 999L, accountId = "acc-1")

        assertEquals(999L, repository.latestLocalChangeAt())
    }

    @Test
    fun `editing a loan payment bumps its updatedAt, which becomes the new watermark`() = runTest {
        insertLoan(updatedAt = 1L)
        insertLoanPayment(updatedAt = 2L)

        db.loan_paymentsQueries.update(
            amount = 999_00L,
            method = "Transfer",
            paidAt = "2026-05-24T09:33:20",
            note = "Corregido",
            updatedAt = 777L,
            paymentId = "pay-1",
        )

        assertEquals(777L, repository.latestLocalChangeAt())
    }

    @Test
    fun `updatedAt of zero returns zero, not null`() = runTest {
        insertAccount(updatedAt = 0L)

        assertEquals(0L, repository.latestLocalChangeAt())
    }

    private fun insertAccount(accountId: String = "acc-1", updatedAt: Long) {
        db.accountsQueries.insert(
            accountId = accountId,
            name = "Yape",
            type = "Cash",
            currency = "PEN",
            updatedAt = updatedAt,
            createdAt = updatedAt,
        )
    }

    private fun insertCategory(categoryId: String = "cat-1", updatedAt: Long) {
        db.categoriesQueries.insert(
            categoryId = categoryId,
            name = "Comida",
            icon = "work",
            color = "#00FF00",
            categoryType = "Spend",
            isDefault = false,
            updatedAt = updatedAt,
            createdAt = updatedAt,
        )
    }

    private fun insertTransaction(transactionId: String = "tx-1", accountId: String = "acc-1", updatedAt: Long) {
        db.transactionsQueries.insert(
            transactionId = transactionId,
            type = "Spend",
            amount = 100_00L,
            description = "Almuerzo",
            occurredAt = "2026-05-23T09:33:20",
            categoryId = null,
            accountId = accountId,
            createdAt = updatedAt,
            updatedAt = updatedAt,
        )
    }

    private fun insertLoan(loanId: String = "loan-1", updatedAt: Long) {
        db.loansQueries.insert(
            loanId = loanId,
            personName = "Rosa",
            personKey = "rosa",
            principal = 300_00L,
            interestBps = 0L,
            totalDue = 300_00L,
            note = "",
            lentAt = "2026-05-23T09:33:20",
            createdAt = updatedAt,
            updatedAt = updatedAt,
        )
    }

    private fun insertLoanPayment(paymentId: String = "pay-1", loanId: String = "loan-1", updatedAt: Long) {
        db.loan_paymentsQueries.insert(
            paymentId = paymentId,
            loanId = loanId,
            amount = 50_00L,
            method = "Cash",
            paidAt = "2026-05-24T09:33:20",
            note = "",
            createdAt = updatedAt,
            updatedAt = updatedAt,
        )
    }

    private fun insertTemplate(id: String = "rec-1", accountId: String = "acc-1", updatedAt: Long) {
        db.recurring_movementsQueries.insert(
            id = id,
            name = "Alquiler",
            type = "Spend",
            amount = 1200_00L,
            description = "Depa",
            categoryId = null,
            accountId = accountId,
            frequency = "Monthly",
            dayOfMonth = 5L,
            isActive = 1L,
            lastConfirmedPeriod = null,
            createdAt = updatedAt,
            updatedAt = updatedAt,
        )
    }
}
