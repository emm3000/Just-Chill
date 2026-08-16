package com.emm.data.backup

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultBackupRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var repository: DefaultBackupRepository

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-11T15:04:05Z")
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
    fun `produces JSON containing all accounts, categories and transactions`() = runTest {
        insertAccount()
        insertCategory(categoryId = "cat-1", name = "Sueldo", categoryType = "Income")
        insertCategory(categoryId = "cat-2", name = "Comida", categoryType = "Spend")
        insertTransaction(
            transactionId = "tx-1",
            type = "Income",
            amount = 4500_00L,
            occurredAt = "2026-05-23T09:33:20",
            categoryId = "cat-1",
        )
        insertTransaction(
            transactionId = "tx-2",
            type = "Spend",
            amount = 150_00L,
            occurredAt = "2026-05-24T13:20:00",
            categoryId = null,
        )

        val json = repository.exportToJson(exportedAt = 1_748_000_000_000L, appVersion = "1.0.0")

        val payload = Json.decodeFromString<ExportPayloadDto>(json)
        assertEquals(3, payload.schemaVersion)
        assertEquals(1_748_000_000_000L, payload.exportedAt)
        assertEquals("1.0.0", payload.appVersion)
        assertEquals(1, payload.accounts.size)
        assertEquals("acc-1", payload.accounts[0].accountId)
        assertEquals("Yape", payload.accounts[0].name)
        assertEquals(setOf("cat-1", "cat-2"), payload.categories.mapTo(mutableSetOf()) { it.categoryId })
        assertEquals(listOf("tx-2", "tx-1"), payload.transactions.map { it.transactionId })
        assertEquals(150_00L, payload.transactions[0].amountCents)
        assertEquals(4500_00L, payload.transactions[1].amountCents)
    }

    @Test
    fun `a categoryId whose category is gone is exported as null`() = runTest {
        insertAccount()
        insertCategory(categoryId = "cat-1", name = "Sueldo", categoryType = "Income")
        insertCategory(categoryId = "cat-deleted", name = "Bono", categoryType = "Income")
        insertTransaction(transactionId = "tx-1", type = "Income", categoryId = "cat-1")
        insertTransaction(transactionId = "tx-3", type = "Income", categoryId = "cat-deleted")
        db.categoriesQueries.softDelete(deletedAt = 1L, updatedAt = 1L, categoryId = "cat-deleted")

        val payload = exportedPayload()

        assertEquals(listOf("cat-1"), payload.categories.map { it.categoryId })
        assertEquals("cat-1", payload.transactions.single { it.transactionId == "tx-1" }.categoryId)
        assertNull(payload.transactions.single { it.transactionId == "tx-3" }.categoryId)
    }

    @Test
    fun `empty state - produces valid JSON with the current schemaVersion and empty arrays`() = runTest {
        val payload = exportedPayload()

        assertEquals(3, payload.schemaVersion)
        assertTrue(payload.accounts.isEmpty())
        assertTrue(payload.categories.isEmpty())
        assertTrue(payload.transactions.isEmpty())
        assertTrue(payload.recurringMovements.isEmpty())
    }

    @Test
    fun `JSON output is pretty-printed`() = runTest {
        val json = repository.exportToJson(exportedAt = 0L, appVersion = "1.0.0")

        assertTrue(json.contains('\n'))
    }

    @Test
    fun `a tombstoned row is not exported, in any of the four tables`() = runTest {
        insertAccount(accountId = "acc-1")
        insertAccount(accountId = "acc-dead")
        insertCategory(categoryId = "cat-1", name = "Sueldo", categoryType = "Income")
        insertCategory(categoryId = "cat-dead", name = "Bono", categoryType = "Income")
        insertTransaction(transactionId = "tx-1", type = "Income", categoryId = "cat-1")
        insertTransaction(transactionId = "tx-dead", type = "Income", categoryId = "cat-1")
        insertTemplate(id = "rec-1")
        insertTemplate(id = "rec-dead")
        db.accountsQueries.softDelete(deletedAt = 1L, updatedAt = 1L, accountId = "acc-dead")
        db.categoriesQueries.softDelete(deletedAt = 1L, updatedAt = 1L, categoryId = "cat-dead")
        db.transactionsQueries.softDelete(deletedAt = 1L, updatedAt = 1L, transactionId = "tx-dead")
        db.recurring_movementsQueries.softDelete(deletedAt = 1L, updatedAt = 1L, id = "rec-dead")

        val payload = exportedPayload()

        assertEquals(listOf("acc-1"), payload.accounts.map { it.accountId })
        assertEquals(listOf("cat-1"), payload.categories.map { it.categoryId })
        assertEquals(listOf("tx-1"), payload.transactions.map { it.transactionId })
        assertEquals(listOf("rec-1"), payload.recurringMovements.map { it.recurringMovementId })
    }

    @Test
    fun `the export carries every live template, the paused ones included`() = runTest {
        insertAccount()
        insertCategory(categoryId = "cat-2", name = "Comida", categoryType = "Spend")
        insertTemplate(id = "rec-1", name = "Alquiler", categoryId = "cat-2")
        insertTemplate(id = "rec-2", name = "Gimnasio", categoryId = "cat-2", isActive = 0L)

        val payload = exportedPayload()

        assertEquals(listOf("rec-1", "rec-2"), payload.recurringMovements.map { it.recurringMovementId })
        assertEquals(listOf(true, false), payload.recurringMovements.map { it.isActive })
        val exported = payload.recurringMovements.first()
        assertEquals("Alquiler", exported.name)
        assertEquals("Spend", exported.type)
        assertEquals(1200_00L, exported.amountCents)
        assertEquals("Depa", exported.description)
        assertEquals("cat-2", exported.categoryId)
        assertEquals("acc-1", exported.accountId)
        assertEquals("Monthly", exported.frequency)
        assertEquals(5, exported.dayOfMonth)
    }

    @Test
    fun `a template with no fixed amount exports a null amount rather than a zero`() = runTest {
        insertAccount()
        insertTemplate(id = "rec-1", amount = null)

        assertNull(exportedPayload().recurringMovements.single().amountCents)
    }

    @Test
    fun `the export carries a template's own settled mark and createdAt`() = runTest {
        insertAccount()
        insertTemplate(id = "rec-1", lastConfirmedPeriod = "2026-07", createdAt = TEMPLATE_CREATED)

        val exported = exportedPayload(exportedAt = EXPORTED_AT).recurringMovements.single()

        assertEquals("2026-07", exported.lastConfirmedPeriod)
        assertEquals(TEMPLATE_CREATED, exported.createdAt)
    }

    @Test
    fun `a template whose category is gone is exported with no category`() = runTest {
        insertAccount()
        insertCategory(categoryId = "cat-1", name = "Comida", categoryType = "Spend")
        insertCategory(categoryId = "cat-deleted", name = "Antojos", categoryType = "Spend")
        insertTemplate(id = "rec-1", categoryId = "cat-deleted")
        insertTemplate(id = "rec-2", name = "Bus", categoryId = "cat-1")
        db.categoriesQueries.softDelete(deletedAt = 1L, updatedAt = 1L, categoryId = "cat-deleted")

        val exported = exportedPayload().recurringMovements

        assertNull(exported.single { it.recurringMovementId == "rec-1" }.categoryId)
        assertEquals("cat-1", exported.single { it.recurringMovementId == "rec-2" }.categoryId)
    }

    @Test
    fun `a database failure during the export surfaces as a DomainException`() = runTest {
        driver.execute(null, "DROP TABLE recurring_movements", 0)

        assertFailsWith<DomainException> {
            repository.exportToJson(exportedAt = 0L, appVersion = "1.0.0")
        }
    }

    @Test
    fun `a serialization failure translates to SerializationError, not Unknown or DatabaseError`() {
        val ex = assertFailsWith<DomainException.SerializationError> {
            encodeAsDomainException<String> { throw SerializationException("boom") }
        }

        assertEquals("boom", ex.message)
    }

    @Test
    fun `every read the export makes runs inside one open transaction`() = runTest {
        val spy = TransactionSpyDriver(driver)
        val spied = DefaultBackupRepository(db = EmmDatabaseData(spy), clock = clock)
        insertAccount()
        insertCategory(categoryId = "cat-1", name = "Sueldo", categoryType = "Income")
        insertTransaction(transactionId = "tx-1", type = "Income", categoryId = "cat-1")
        insertTemplate(id = "rec-1")

        spied.exportToJson(exportedAt = 0L, appVersion = "1.0.0")

        assertEquals(listOf(true, true, true, true), spy.readsInsideTransaction)
    }

    private class TransactionSpyDriver(private val delegate: SqlDriver) : SqlDriver by delegate {

        val readsInsideTransaction: MutableList<Boolean> = mutableListOf()

        override fun <R> executeQuery(
            identifier: Int?,
            sql: String,
            mapper: (SqlCursor) -> QueryResult<R>,
            parameters: Int,
            binders: (SqlPreparedStatement.() -> Unit)?,
        ): QueryResult<R> {
            readsInsideTransaction += delegate.currentTransaction() != null
            return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
        }
    }

    private suspend fun exportedPayload(exportedAt: Long = 0L): ExportPayloadDto =
        Json.decodeFromString(repository.exportToJson(exportedAt = exportedAt, appVersion = "1.0.0"))

    private fun insertAccount(accountId: String = "acc-1", name: String = "Yape") {
        db.accountsQueries.insert(
            accountId = accountId,
            name = name,
            type = "Cash",
            currency = "PEN",
            updatedAt = 0L,
            createdAt = 0L,
        )
    }

    private fun insertCategory(categoryId: String, name: String, categoryType: String) {
        db.categoriesQueries.insert(
            categoryId = categoryId,
            name = name,
            icon = "work",
            color = "#00FF00",
            categoryType = categoryType,
            isDefault = false,
            updatedAt = 0L,
            createdAt = 0L,
        )
    }

    private fun insertTransaction(
        transactionId: String,
        type: String,
        amount: Long = 100_00L,
        occurredAt: String = "2026-05-23T09:33:20",
        categoryId: String?,
        accountId: String = "acc-1",
    ) {
        db.transactionsQueries.insert(
            transactionId = transactionId,
            type = type,
            amount = amount,
            description = "Sueldo mayo",
            occurredAt = occurredAt,
            categoryId = categoryId,
            accountId = accountId,
            createdAt = 0L,
            updatedAt = 0L,
        )
    }

    private fun insertTemplate(
        id: String,
        name: String = "Alquiler",
        type: String = "Spend",
        amount: Long? = 1200_00L,
        categoryId: String? = null,
        isActive: Long = 1L,
        lastConfirmedPeriod: String? = null,
        createdAt: Long = TEMPLATE_CREATED,
    ) {
        db.recurring_movementsQueries.insert(
            id = id,
            name = name,
            type = type,
            amount = amount,
            description = "Depa",
            categoryId = categoryId,
            accountId = "acc-1",
            frequency = "Monthly",
            dayOfMonth = 5L,
            isActive = isActive,
            lastConfirmedPeriod = lastConfirmedPeriod,
            createdAt = createdAt,
            updatedAt = 0L,
        )
    }

    private companion object {
        val TEMPLATE_CREATED: Long = Instant.parse("2026-06-10T12:00:00Z").toEpochMilliseconds()

        val EXPORTED_AT: Long = Instant.parse("2026-08-11T15:04:05Z").toEpochMilliseconds()
    }
}
