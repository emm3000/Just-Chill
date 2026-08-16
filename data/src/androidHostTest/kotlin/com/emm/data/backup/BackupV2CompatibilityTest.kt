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
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class BackupV2CompatibilityTest {

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
                override fun now(): Instant = Instant.parse("2026-08-11T15:04:05Z")
            },
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `the frozen version 2 shape parses the bytes a version 2 export actually wrote`() {
        val payload = importJson.decodeFromString<ExportPayloadV2Dto>(V2_BACKUP)

        assertEquals(BACKUP_SCHEMA_VERSION_V2, payload.schemaVersion)
        assertEquals(1_748_000_000_000L, payload.exportedAt)
        assertEquals("2.4.0", payload.appVersion)
        assertEquals(listOf("acc-1"), payload.accounts.map { it.accountId })
        assertEquals(listOf("cat-income", "cat-spend"), payload.categories.map { it.categoryId })
        assertEquals(listOf("tx-1", "tx-2", "tx-3"), payload.transactions.map { it.transactionId })
        assertEquals("2025-05-23T06:33:20", payload.transactions[0].occurredAt)
        assertNull(payload.transactions[2].categoryId)
        assertEquals("PEN", payload.accounts[0].currency)
    }

    @Test
    fun `toCurrent carries every version 2 field across and invents only the list version 2 lacked`() {
        val frozen = importJson.decodeFromString<ExportPayloadV2Dto>(V2_BACKUP)

        val current = frozen.toCurrent()

        assertEquals(BACKUP_SCHEMA_VERSION, current.schemaVersion)
        assertEquals(frozen.exportedAt, current.exportedAt)
        assertEquals(frozen.appVersion, current.appVersion)
        assertEquals(frozen.accounts, current.accounts)
        assertEquals(frozen.categories, current.categories)
        assertEquals(frozen.transactions, current.transactions)
        assertTrue(current.recurringMovements.isEmpty())
    }

    @Test
    fun `a version 2 file reaches the import still declaring version 2`() {
        val decoded = repository.decodePayload(V2_BACKUP)

        assertEquals(BACKUP_SCHEMA_VERSION_V2, decoded.declaredVersion)
        assertNotEquals(BACKUP_SCHEMA_VERSION, decoded.declaredVersion)
        assertEquals(BACKUP_SCHEMA_VERSION, decoded.payload.schemaVersion)
        assertTrue(decoded.payload.recurringMovements.isEmpty())
    }

    @Test
    fun `a version 2 file still restores everything it carries`() = runTest {
        val stats = repository.importFromJson(V2_BACKUP)

        assertEquals(ImportStats(accounts = 1, categories = 2, transactions = 3, recurring = 0), stats)
        assertEquals(1, db.accountsQueries.all().executeAsList().size)
        assertEquals(2, db.categoriesQueries.all().executeAsList().size)
        assertEquals(3, db.transactionsQueries.all().executeAsList().size)
    }

    @Test
    fun `a version 2 occurrence is stored as the same local text the file carried`() = runTest {
        repository.importFromJson(V2_BACKUP)

        val tx = db.transactionsQueries.find("tx-1").executeAsOne()
        assertEquals("2025-05-23T06:33:20", tx.occurredAt)
        assertEquals(450_000L, tx.amount)
        assertEquals("Sueldo mayo", tx.description)
        assertEquals("cat-income", tx.categoryId)
    }

    @Test
    fun `a version 2 midnight stays at midnight rather than sliding a day`() = runTest {
        repository.importFromJson(V2_BACKUP)

        assertEquals("2025-05-23T00:00:00", db.transactionsQueries.find("tx-2").executeAsOne().occurredAt)
    }

    @Test
    fun `a version 2 transaction with no category restores uncategorized`() = runTest {
        repository.importFromJson(V2_BACKUP)

        val tx = db.transactionsQueries.find("tx-3").executeAsOne()
        assertNull(tx.categoryId)
        assertEquals("2025-05-24T10:20:00", tx.occurredAt)
    }

    @Test
    fun `a version the app has never written is still refused`() = runTest {
        val fromTheFuture = V2_BACKUP.replace("\"schemaVersion\": 2", "\"schemaVersion\": 99")

        val ex = assertFailsWith<DomainException.ValidationError> { repository.importFromJson(fromTheFuture) }

        assertEquals(ValidationCode.BackupVersionUnsupported, ex.code)
    }

    @Test
    fun `importing a version 2 file leaves the recurring movements already on the device alive`() = runTest {
        repository.importFromJson(V2_BACKUP)
        exec(
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, " +
                "accountId, dayOfMonth, createdAt, updatedAt) " +
                "VALUES ('rec-1', 'Alquiler', 'Spend', 120000, '', 'cat-spend', 'acc-1', 5, 1, 1)",
        )

        repository.importFromJson(V2_BACKUP)

        assertEquals(1, rawCount("SELECT COUNT(*) FROM recurring_movements"), "the row was deleted outright")
        assertEquals(
            0,
            rawCount("SELECT COUNT(*) FROM recurring_movements WHERE deletedAt IS NOT NULL"),
            "the row was tombstoned by the import's sweep",
        )
        val recurring = db.recurring_movementsQueries.find("rec-1").executeAsOne()
        assertEquals("Alquiler", recurring.name)
        assertEquals(120_000L, recurring.amount)
        assertEquals("cat-spend", recurring.categoryId)
        assertEquals(1L, recurring.isActive)
    }

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun rawCount(sql: String): Long = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0))
        },
        parameters = 0,
    ).value ?: 0L

    private companion object {

        val importJson = Json { ignoreUnknownKeys = true }

        const val V2_BACKUP = """
{
    "schemaVersion": 2,
    "exportedAt": 1748000000000,
    "appVersion": "2.4.0",
    "accounts": [
        {
            "accountId": "acc-1",
            "name": "BCP",
            "type": "Bank",
            "currency": "PEN"
        }
    ],
    "categories": [
        {
            "categoryId": "cat-income",
            "name": "Sueldo",
            "icon": "salary",
            "color": "green",
            "categoryType": "Income"
        },
        {
            "categoryId": "cat-spend",
            "name": "Comida",
            "icon": "food",
            "color": "red",
            "categoryType": "Spend"
        }
    ],
    "transactions": [
        {
            "transactionId": "tx-1",
            "type": "Income",
            "amountCents": 450000,
            "description": "Sueldo mayo",
            "occurredAt": "2025-05-23T06:33:20",
            "accountId": "acc-1",
            "categoryId": "cat-income"
        },
        {
            "transactionId": "tx-2",
            "type": "Spend",
            "amountCents": 8540,
            "description": "Mercado",
            "occurredAt": "2025-05-23T00:00:00",
            "accountId": "acc-1",
            "categoryId": "cat-spend"
        },
        {
            "transactionId": "tx-3",
            "type": "Spend",
            "amountCents": 1200,
            "description": "Café",
            "occurredAt": "2025-05-24T10:20:00",
            "accountId": "acc-1",
            "categoryId": null
        }
    ]
}
"""
    }
}
