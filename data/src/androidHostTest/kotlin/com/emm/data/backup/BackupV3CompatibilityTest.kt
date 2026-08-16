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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class BackupV3CompatibilityTest {

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
    fun `the current shape parses the bytes a version 3 export writes`() = runTest {
        val payload = importJson.decodeFromString<ExportPayloadDto>(V3_BACKUP)

        assertEquals(BACKUP_SCHEMA_VERSION, payload.schemaVersion)
        assertEquals(listOf("acc-1"), payload.accounts.map { it.accountId })
        assertEquals(listOf("cat-income", "cat-spend"), payload.categories.map { it.categoryId })
        assertEquals(listOf("tx-1"), payload.transactions.map { it.transactionId })
        assertEquals(listOf("rec-1", "rec-2"), payload.recurringMovements.map { it.recurringMovementId })
    }

    @Test
    fun `every field of a version 3 template survives the round trip through the file`() = runTest {
        val templates = importJson.decodeFromString<ExportPayloadDto>(V3_BACKUP).recurringMovements

        val rent = templates.single { it.recurringMovementId == "rec-1" }
        assertEquals("Alquiler", rent.name)
        assertEquals("Spend", rent.type)
        assertEquals(120_000L, rent.amountCents)
        assertEquals("Depa en Miraflores", rent.description)
        assertEquals("cat-spend", rent.categoryId)
        assertEquals("acc-1", rent.accountId)
        assertEquals("Monthly", rent.frequency)
        assertEquals(5, rent.dayOfMonth)
        assertEquals(true, rent.isActive)
        assertEquals("2026-07", rent.lastConfirmedPeriod)
        assertEquals(1_780_000_000_000L, rent.createdAt)
    }

    @Test
    fun `a version 3 template with no amount and no category reads as null on both`() = runTest {
        val paused = importJson.decodeFromString<ExportPayloadDto>(V3_BACKUP)
            .recurringMovements
            .single { it.recurringMovementId == "rec-2" }

        assertNull(paused.amountCents)
        assertNull(paused.categoryId)
        assertNull(paused.lastConfirmedPeriod)
        assertEquals(false, paused.isActive)
    }

    @Test
    fun `a version 3 file with templates declares 3 and carries them through the decode`() {
        val decoded = decodeBackupPayload(V3_BACKUP)

        assertEquals(BACKUP_SCHEMA_VERSION, decoded.declaredVersion)
        assertEquals(BACKUP_SCHEMA_VERSION, decoded.payload.schemaVersion)
        assertEquals(listOf("rec-1", "rec-2"), decoded.payload.recurringMovements.map { it.recurringMovementId })
    }

    @Test
    fun `a version 3 file with no templates is told from a v1 or v2 file only by its declared version`() {
        val decoded = decodeBackupPayload(V3_BACKUP_EMPTY_RECURRING)

        assertEquals(BACKUP_SCHEMA_VERSION, decoded.declaredVersion)
        assertEquals(BACKUP_SCHEMA_VERSION, decoded.payload.schemaVersion)
        assertTrue(decoded.payload.recurringMovements.isEmpty())
        assertEquals(listOf("tx-1"), decoded.payload.transactions.map { it.transactionId })
    }

    @Test
    fun `a version 3 file with no recurringMovements key is refused as corrupt, not read as none`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            repository.importFromJson(V3_BACKUP_WITHOUT_RECURRING)
        }

        assertEquals(ValidationCode.BackupFileInvalid, ex.code)
    }

    @Test
    fun `a version 3 file still restores the three tables the older versions carried`() = runTest {
        val stats = repository.importFromJson(V3_BACKUP)

        assertEquals(ImportStats(accounts = 1, categories = 2, transactions = 1, recurring = 2), stats)
        assertEquals(1, db.accountsQueries.all().executeAsList().size)
        assertEquals(2, db.categoriesQueries.all().executeAsList().size)
        assertEquals(1, db.transactionsQueries.all().executeAsList().size)
    }

    @Test
    fun `importing a version 3 file replaces the templates on the device`() = runTest {
        repository.importFromJson(V3_BACKUP)
        exec(
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, " +
                "accountId, dayOfMonth, isActive, lastConfirmedPeriod, createdAt, updatedAt) " +
                "VALUES ('on-device', 'Netflix', 'Spend', 4490, 'Plan', 'cat-spend', 'acc-1', 12, 1, " +
                "'2026-06', 1780000000000, 1780000000000)",
        )

        repository.importFromJson(V3_BACKUP)

        assertEquals(3, rawCount("SELECT COUNT(*) FROM recurring_movements"), "a row was deleted outright")
        assertEquals(
            1,
            rawCount("SELECT COUNT(*) FROM recurring_movements WHERE id = 'on-device' AND deletedAt IS NOT NULL"),
            "the row the file does not carry was left live",
        )
        val rent = db.recurring_movementsQueries.find("rec-1").executeAsOne()
        assertEquals("Alquiler", rent.name)
        assertEquals(120_000L, rent.amount)
        assertEquals("cat-spend", rent.categoryId)
        assertEquals(1L, rent.isActive)
        assertEquals("2026-07", rent.lastConfirmedPeriod)
        assertEquals(1_780_000_000_000L, rent.createdAt)
        assertEquals(0L, db.recurring_movementsQueries.find("rec-2").executeAsOne().isActive)
    }

    @Test
    fun `a version 3 file carrying no templates still sweeps the ones on the device`() = runTest {
        repository.importFromJson(V3_BACKUP)
        assertEquals(2, rawCount("SELECT COUNT(*) FROM recurring_movements WHERE deletedAt IS NULL"))

        repository.importFromJson(V3_BACKUP_EMPTY_RECURRING)

        assertEquals(2, rawCount("SELECT COUNT(*) FROM recurring_movements"), "a row was deleted outright")
        assertEquals(
            2,
            rawCount("SELECT COUNT(*) FROM recurring_movements WHERE deletedAt IS NOT NULL"),
            "the file says the device owns no templates and the sweep did not run",
        )
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

        const val V3_BACKUP = """
{
    "schemaVersion": 3,
    "exportedAt": 1785000000000,
    "appVersion": "2.5.0",
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
        }
    ],
    "recurringMovements": [
        {
            "recurringMovementId": "rec-1",
            "name": "Alquiler",
            "type": "Spend",
            "amountCents": 120000,
            "description": "Depa en Miraflores",
            "categoryId": "cat-spend",
            "accountId": "acc-1",
            "frequency": "Monthly",
            "dayOfMonth": 5,
            "isActive": true,
            "lastConfirmedPeriod": "2026-07",
            "createdAt": 1780000000000
        },
        {
            "recurringMovementId": "rec-2",
            "name": "Gimnasio",
            "type": "Spend",
            "amountCents": null,
            "description": "",
            "categoryId": null,
            "accountId": "acc-1",
            "frequency": "Monthly",
            "dayOfMonth": 1,
            "isActive": false,
            "lastConfirmedPeriod": null,
            "createdAt": 1780000000000
        }
    ]
}
"""

        const val V3_BACKUP_EMPTY_RECURRING = """
{
    "schemaVersion": 3,
    "exportedAt": 1785000000000,
    "appVersion": "2.5.0",
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
        }
    ],
    "recurringMovements": []
}
"""

        const val V3_BACKUP_WITHOUT_RECURRING = """
{
    "schemaVersion": 3,
    "exportedAt": 1785000000000,
    "appVersion": "2.5.0",
    "accounts": [
        {
            "accountId": "acc-1",
            "name": "BCP",
            "type": "Bank",
            "currency": "PEN"
        }
    ],
    "categories": [],
    "transactions": []
}
"""
    }
}
