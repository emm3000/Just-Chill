package com.emm.data.backup

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.CategoryRepository
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.backup.ImportStats
import com.emm.domain.transaction.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The bytes schema version 3 writes, and what reading them back is allowed to do today.
 *
 * Version 3 is where the format started carrying `recurringMovements`. The suite is the frozen
 * record of that shape — field names included — so that a rename in [RecurringMovementDto] fails
 * here rather than on a file in the user's storage a year from now.
 *
 * **The fixture below is written out by hand and must never be generated from a DTO.** A fixture
 * built from the current types is the test that stays green straight through the breaking change it
 * was supposed to catch: rename the field and the fixture renames itself. These are literal bytes.
 * If a change makes this suite fail, the change breaks real files on a real disk — fix the reader,
 * never the fixture.
 *
 * ### What is deliberately NOT here yet
 *
 * The import does not restore recurring movements, and does not sweep them either. The format
 * carrying them and the import acting on them are two commits, in that order, on purpose: the
 * version number has to move with the shape, and the destructive half — tombstone every live
 * template, then put back exactly what the file holds — is its own change with its own risk.
 *
 * So a v3 file today carries templates that importing does not put back. That is an intermediate
 * state, and the guard below is what keeps it from being a destructive one: a v3 import must neither
 * tombstone nor delete the templates already on the device. Note the claim is narrow on purpose — an
 * import can still detach a template's category, and the guard's own KDoc says exactly when. When the
 * sweep lands, that test is the one that has to be deliberately inverted, which is the point of it
 * existing now.
 */
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
            transactions = mockk<TransactionRepository> { every { all() } returns flowOf(emptyList()) },
            categories = mockk<CategoryRepository> { every { all() } returns flowOf(emptyList()) },
            accounts = mockk<AccountRepository> { every { all() } returns flowOf(emptyList<Account>()) },
            recurring = mockk<RecurringMovementRepository> { every { allLive() } returns flowOf(emptyList()) },
            db = db,
            // Stated, not read: the storage stamps this import writes must not move with the machine
            // the suite runs on.
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
        // The two that are behaviour rather than storage — see RecurringMovementDto.
        assertEquals("2026-07", rent.lastConfirmedPeriod)
        assertEquals(1_780_000_000_000L, rent.createdAt)
    }

    @Test
    fun `a version 3 template with no amount and no category reads as null on both`() = runTest {
        val paused = importJson.decodeFromString<ExportPayloadDto>(V3_BACKUP)
            .recurringMovements
            .single { it.recurringMovementId == "rec-2" }

        // Nullable columns, and both readings are load-bearing: "no amount agreed" is not zero, and
        // an uncategorized template satisfies the composite key precisely because the column is null.
        assertNull(paused.amountCents)
        assertNull(paused.categoryId)
        assertNull(paused.lastConfirmedPeriod)
        assertEquals(false, paused.isActive)
    }

    @Test
    fun `a version 3 file still restores the three tables the older versions carried`() = runTest {
        val stats = repository.importFromJson(V3_BACKUP)

        assertEquals(ImportStats(accounts = 1, categories = 2, transactions = 1), stats)
        assertEquals(1, db.accountsQueries.all().executeAsList().size)
        assertEquals(2, db.categoriesQueries.all().executeAsList().size)
        assertEquals(1, db.transactionsQueries.all().executeAsList().size)
    }

    /**
     * The intermediate state, pinned so it cannot become a silent one.
     *
     * The format carries templates; the import neither sweeps nor restores them. Both halves have to
     * be true at once and only one of them is visible from the file — so what this asserts is the
     * invisible half: no row is tombstoned, no row is deleted, and the file's own two templates land
     * nowhere.
     *
     * **"Untouched" would be the wrong word, and it is worth being exact about why.** A v3 import can
     * still mutate this table, through one narrow path that has nothing to do with the new field:
     * `restore(dto: CategoryDto, …)` calls `clearCategoryOnTypeChange` on `recurring_movementsQueries`
     * for every category the file restores, nulling `categoryId` on any recurring row whose `type`
     * disagrees with that category's `categoryType`. It has to — SQLite refuses the parent-key change
     * otherwise, and inside the one transaction wrapping the restore that is the whole import rolled
     * back rather than one failed row. This fixture does not trigger it because the on-device row
     * holds `(cat-spend, Spend)` and the file restores `cat-spend` as `Spend`: the pair agrees, so
     * the detach's `type != :categoryType` matches nothing. `DefaultBackupRepositoryImportTest` owns
     * the case where it does fire.
     *
     * The claim here is therefore the narrow one — no sweep, no restore — and it is the one commit ③
     * has to deliberately invert for v3 while keeping it true for v1 and v2.
     *
     * The destructive shapes are checked first, through raw SQL that sees every row. `find` filters
     * `deletedAt IS NULL`, so it cannot tell "physically deleted" from "tombstoned", and asking it
     * for `deletedAt` would be vacuous since it can never return a row where that is set. Asserting
     * these before the read is what makes each mode fail with its own message instead of both
     * arriving as an NPE out of `executeAsOne`.
     */
    @Test
    fun `importing a version 3 file neither sweeps nor restores the templates on the device`() = runTest {
        repository.importFromJson(V3_BACKUP)
        exec(
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, " +
                "accountId, dayOfMonth, isActive, lastConfirmedPeriod, createdAt, updatedAt) " +
                "VALUES ('on-device', 'Netflix', 'Spend', 4490, 'Plan', 'cat-spend', 'acc-1', 12, 1, " +
                "'2026-06', 1780000000000, 1780000000000)",
        )

        repository.importFromJson(V3_BACKUP)

        assertEquals(1, rawCount("SELECT COUNT(*) FROM recurring_movements"), "the row was deleted outright")
        assertEquals(
            0,
            rawCount("SELECT COUNT(*) FROM recurring_movements WHERE deletedAt IS NOT NULL"),
            "the row was tombstoned by the import's sweep",
        )
        // The file carries two templates and neither of them is this one: the import restored
        // nothing, so the device's own row is still exactly what it was.
        val onDevice = db.recurring_movementsQueries.find("on-device").executeAsOne()
        assertEquals("Netflix", onDevice.name)
        assertEquals(4490L, onDevice.amount)
        assertEquals("cat-spend", onDevice.categoryId)
        assertEquals(1L, onDevice.isActive)
        assertEquals("2026-06", onDevice.lastConfirmedPeriod)
        assertEquals(1_780_000_000_000L, onDevice.createdAt)
        assertEquals(0, rawCount("SELECT COUNT(*) FROM recurring_movements WHERE id IN ('rec-1','rec-2')"))
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

        /**
         * Configured like `DefaultBackupRepository`'s own private `importJson`, so the direct-decode
         * tests read the fixture under the same leniency the real import path uses.
         */
        val importJson = Json { ignoreUnknownKeys = true }

        /**
         * A version-3 export, byte for byte. Do not regenerate this from [ExportPayloadDto] — see
         * the class doc. `rec-2` is paused, unpriced, uncategorized and never confirmed on purpose:
         * every nullable field in the shape is exercised by one row or the other.
         */
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
    }
}
