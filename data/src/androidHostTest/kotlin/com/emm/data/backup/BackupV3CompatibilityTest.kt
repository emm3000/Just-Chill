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
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
 * ### The version this suite is the gate for
 *
 * Version 3 is the first version an import is allowed to act on: it tombstones every live template
 * and then puts back exactly the ones the file carries. `BackupV1CompatibilityTest` and
 * `BackupV2CompatibilityTest` hold the other side — an older file must leave that table's rows
 * alive, because it has none to give back.
 *
 * The two suites are one guard, not two, and the second test below is the half that makes it mean
 * something: a v3 export from a device that owns no templates is byte-identical to a converted v1 or
 * v2 payload in everything but its declared version, and it must still sweep. An import that gated
 * on `recurringMovements` being empty would pass every other test in this file and get exactly that
 * one backwards, in both directions.
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

    /**
     * A v3 file that carries templates reaches the import declaring 3, with its templates intact.
     *
     * The claim is only that: the version survives the decode and so does the list. It is NOT the
     * "only the declared version can tell this file apart" claim its twins in
     * `BackupV1CompatibilityTest` and `BackupV2CompatibilityTest` make — a non-empty
     * `recurringMovements` is itself a v3 discriminator, since no `toCurrent()` ever produces one.
     * The test below owns that claim, against the file that can actually exercise it.
     */
    @Test
    fun `a version 3 file with templates declares 3 and carries them through the decode`() {
        val decoded = repository.decodePayload(V3_BACKUP)

        assertEquals(BACKUP_SCHEMA_VERSION, decoded.declaredVersion)
        assertEquals(BACKUP_SCHEMA_VERSION, decoded.payload.schemaVersion)
        assertEquals(listOf("rec-1", "rec-2"), decoded.payload.recurringMovements.map { it.recurringMovementId })
    }

    /**
     * The third of the three declared-version guards, and the one that makes the other two mean
     * something: a v3 export from a device with no templates is indistinguishable from a converted
     * v1 or v2 payload, and must still arrive declaring 3.
     *
     * The three assertions below are exactly the object `ExportPayloadV1Dto.toCurrent()` and
     * `ExportPayloadV2Dto.toCurrent()` hand over — current `schemaVersion`, empty
     * `recurringMovements` — with one difference the payload has no field for. So the pair is the
     * point: emptiness here is the user's data saying "none", emptiness there is a format that never
     * carried any, and `declaredVersion` is the only thing between them.
     *
     * Getting that backwards is what the version-gated sweep must not do. A sweep that inferred the
     * format from the list would read this file as pre-v3 and skip it — leaving on the device
     * exactly the templates the owner's backup says are gone — and would read a v1 file as v3 the
     * moment it inferred the other way, tombstoning templates no v1 file can put back.
     */
    @Test
    fun `a version 3 file with no templates is told from a v1 or v2 file only by its declared version`() {
        val decoded = repository.decodePayload(V3_BACKUP_EMPTY_RECURRING)

        assertEquals(BACKUP_SCHEMA_VERSION, decoded.declaredVersion)
        assertEquals(BACKUP_SCHEMA_VERSION, decoded.payload.schemaVersion)
        assertTrue(decoded.payload.recurringMovements.isEmpty())
        // The rest of the file decoded too, so the emptiness above is the file's statement about
        // templates and not a decode that failed its way into a blank payload.
        assertEquals(listOf("tx-1"), decoded.payload.transactions.map { it.transactionId })
    }

    /**
     * A file that says version 3 and carries no `recurringMovements` key is corrupt, and is refused
     * as corrupt — never decoded as "there are none".
     *
     * This is what [ExportPayloadDto.recurringMovements] having **no default** buys, and it is the
     * whole of what it buys: adding `= emptyList()` is a one-token edit that compiles clean and
     * leaves every other test in this module green, while turning a malformed file into a file that
     * claims the device owns no templates. Under the version-gated sweep that claim is a delete
     * order for every template on the device, authorized by a key that was simply missing.
     *
     * The fixture is the real one minus that key, written out by hand for the same reason the rest
     * of this suite is: a variant derived from [ExportPayloadDto] would follow a rename.
     */
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

        // Both rec-1 and rec-2 carry a type and a frequency this build knows, so both land.
        assertEquals(ImportStats(accounts = 1, categories = 2, transactions = 1, recurring = 2), stats)
        assertEquals(1, db.accountsQueries.all().executeAsList().size)
        assertEquals(2, db.categoriesQueries.all().executeAsList().size)
        assertEquals(1, db.transactionsQueries.all().executeAsList().size)
    }

    /**
     * "Make everything look like this file", now including the templates.
     *
     * A template the file does not mention is **tombstoned, never deleted** — the same rule the other
     * three tables follow, and the reason is not cosmetic: a physical DELETE leaves no trace for the
     * push to carry, so the row comes back on the next pull, and `ON DELETE RESTRICT` would refuse it
     * anyway while anything still references the account. The destructive shapes are therefore
     * checked through raw SQL, which sees every row: `find` filters `deletedAt IS NULL` and so cannot
     * tell "deleted" from "tombstoned" at all.
     *
     * The file's own two templates land with the bytes the file carried — `lastConfirmedPeriod` and
     * `createdAt` included, the two fields whose loss is the whole reason version 3 exists.
     */
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
        // The paused one lands too: a template the user stopped is still data the user owns.
        assertEquals(0L, db.recurring_movementsQueries.find("rec-2").executeAsOne().isActive)
    }

    /**
     * **The one test an empty-list gate cannot pass**, and the reason the gate reads the declared
     * version instead.
     *
     * This file is a real v3 export from a device that owns no templates. After the decode it is
     * indistinguishable from a converted v1 or v2 payload — current `schemaVersion`, empty
     * `recurringMovements` — and yet the correct answer is the opposite one: the owner's backup says
     * there are none, so the templates on the device go. An import that inferred the format from the
     * list would skip this file and leave behind exactly the templates the backup says are gone,
     * while reading a v1 file as v3 the moment it inferred the other way.
     *
     * Its twins are `BackupV1CompatibilityTest` and `BackupV2CompatibilityTest`, which hand the
     * import the same empty list and demand the opposite behaviour. Neither test means anything
     * without the other.
     */
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

        /**
         * A version-3 export written by a device that owns no templates: the key is present and the
         * list is empty. Written out by hand like the rest, and deliberately the same three tables
         * as [V3_BACKUP] so the ONLY difference between the two files is the templates.
         *
         * This is the file the declared-version guard needs and [V3_BACKUP] cannot supply. Not to be
         * confused with [V3_BACKUP_WITHOUT_RECURRING], where the key is absent: that one is corrupt
         * and must be refused, this one is a valid export and must decode.
         */
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

        /**
         * A file declaring version 3 with the `recurringMovements` key absent — the shape this app
         * has never written, and the one that must fail rather than decode.
         *
         * Written out by hand like every fixture here, and kept minimal on purpose: the three older
         * lists are present and valid so that the ONLY reason the decode fails is the missing key.
         */
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
