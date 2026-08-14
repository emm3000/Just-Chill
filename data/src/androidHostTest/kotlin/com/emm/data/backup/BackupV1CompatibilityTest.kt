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
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A backup file written by the app BEFORE a transaction's occurrence stopped being an instant must
 * keep restoring, forever.
 *
 * These files are the data escape hatch. They sit in storage the user chose, outside the app, where
 * no schema migration can ever reach them — and if one stops loading, the error the user sees is
 * *"El archivo está dañado o no es un respaldo de JustChill"*: the app calling their intact file
 * corrupt. That is the failure this suite exists to prevent.
 *
 * **The fixture below is written out by hand and must never be generated from a DTO.** A fixture
 * built from the current types is the test that stays green straight through the breaking change
 * it was supposed to catch: rename the field and the fixture renames itself. These are literal
 * bytes, frozen. If a change makes this suite fail, the change breaks real files on a real disk —
 * fix the reader, never the fixture.
 */
class BackupV1CompatibilityTest {

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
            // Stated, not read. This suite asserts `occurredAt`, which comes off the file — but the
            // storage stamps beside it come off this clock, and none of them should move with the
            // machine the suite runs on.
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
    fun `a version 1 file still restores everything it carries`() = runTest {
        val stats = repository.importFromJson(V1_BACKUP)

        // recurring = 0: not "none carried" but "none touched" — a v1 file cannot sweep or restore
        // that table at all, see `importing a version 1 file leaves the recurring movements
        // already on the device alive` below.
        assertEquals(ImportStats(accounts = 1, categories = 2, transactions = 3, recurring = 0), stats)
        assertEquals(1, db.accountsQueries.all().executeAsList().size)
        assertEquals(2, db.categoriesQueries.all().executeAsList().size)
        assertEquals(3, db.transactionsQueries.all().executeAsList().size)
    }

    @Test
    fun `a version 1 instant becomes the Lima wall clock it was recorded at`() = runTest {
        repository.importFromJson(V1_BACKUP)

        // 1_748_000_000_000 ms is 2025-05-23 11:33:20 UTC, which is 06:33:20 in Lima — the same
        // fixed offset the 3 → 4 schema migration applies, so a row restored from a file and the
        // same row migrated in place land on the same value.
        val tx = db.transactionsQueries.find("tx-1").executeAsOne()
        assertEquals("2025-05-23T06:33:20", tx.occurredAt)
        assertEquals(450_000L, tx.amount)
        assertEquals("Sueldo mayo", tx.description)
        assertEquals("cat-income", tx.categoryId)
    }

    @Test
    fun `a version 1 midnight stays at midnight rather than sliding a day`() = runTest {
        repository.importFromJson(V1_BACKUP)

        // 1_747_976_400_000 ms is 2025-05-23 05:00:00 UTC = 2025-05-23 00:00:00 in Lima. Local
        // midnight is the value most likely to cross a day boundary if the offset is wrong in
        // either direction, so it is the one worth pinning.
        assertEquals("2025-05-23T00:00:00", db.transactionsQueries.find("tx-2").executeAsOne().occurredAt)
    }

    @Test
    fun `a version 1 transaction with no category restores uncategorized`() = runTest {
        repository.importFromJson(V1_BACKUP)

        val tx = db.transactionsQueries.find("tx-3").executeAsOne()
        assertEquals(null, tx.categoryId)
        assertEquals("2025-05-24T10:20:00", tx.occurredAt)
    }

    @Test
    fun `a version the app has never written is still refused`() = runTest {
        val fromTheFuture = V1_BACKUP.replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")

        val ex = assertFailsWith<DomainException.ValidationError> { repository.importFromJson(fromTheFuture) }

        assertEquals(ValidationCode.BackupVersionUnsupported, ex.code)
    }

    /**
     * The decode keeps the fact the conversion destroys: this file was written at version 1.
     *
     * `toCurrent()` restamps the payload to the current version — correctly, since the payload is
     * the current shape by then — which leaves a v1 file, a v2 file and a v3 file exported by a
     * device with no templates as three identical objects. `declaredVersion` is what separates them,
     * and the version-gated sweep is the reason that separation has to survive the decode: a sweep
     * that read the empty `recurringMovements` as "the file says there are none" would tombstone
     * every template on the device over a file whose format never carried one.
     */
    @Test
    fun `a version 1 file reaches the import still declaring version 1`() {
        val decoded = repository.decodePayload(V1_BACKUP)

        assertEquals(BACKUP_SCHEMA_VERSION_V1, decoded.declaredVersion)
        // Not the current version — asserting both is the whole point: the payload says 3 for every
        // file that ever restores, so only the declared half can tell this file from a v3 one.
        assertNotEquals(BACKUP_SCHEMA_VERSION, decoded.declaredVersion)
        assertEquals(BACKUP_SCHEMA_VERSION, decoded.payload.schemaVersion)
        assertTrue(decoded.payload.recurringMovements.isEmpty())
    }

    /**
     * The twin of `BackupV2CompatibilityTest`'s recurring guard, and it is not redundant with it.
     *
     * The v1 *format* predates `recurring_movements`, which is a fact about the file and says nothing
     * about the device. The rows are on the device either way: a v1 backup restored onto a phone full
     * of templates is exactly as reachable as a v2 one, and it reaches a different `decodePayload`
     * branch on the way. Once commit ③ makes the sweep version-gated, this is the test that says the
     * gate covers version 1 too — a gate written as "not version 2" would pass its v2 twin and wipe
     * here.
     *
     * The rule, narrow on purpose for the same reason as the v2 twin: **a file older than version 3
     * neither tombstones nor deletes a recurring movement.** Not "leaves the table alone" — the
     * category detach in `restore(dto: CategoryDto, …)` runs on every version, and this fixture avoids
     * it the same way, by restoring `cat-spend` as the `Spend` the on-device row already holds.
     */
    @Test
    fun `importing a version 1 file leaves the recurring movements already on the device alive`() = runTest {
        // The account and category the template hangs off have to exist before it does: the FK to
        // accounts is real, and so is the composite (categoryId, type) key into categories.
        repository.importFromJson(V1_BACKUP)
        exec(
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, " +
                "accountId, dayOfMonth, createdAt, updatedAt) " +
                "VALUES ('rec-1', 'Alquiler', 'Spend', 120000, '', 'cat-spend', 'acc-1', 5, 1, 1)",
        )

        repository.importFromJson(V1_BACKUP)

        // The two destructive shapes first, through raw SQL that sees every row — `find` filters
        // `deletedAt IS NULL`, so it cannot tell "physically deleted" from "tombstoned". Asserting
        // these before the read is what makes each mode fail with its own message instead of both
        // arriving as an NPE out of `executeAsOne`.
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

        /**
         * A real version-1 export, byte for byte: `"date"` as epoch millis, no `"occurredAt"`
         * anywhere. Do not regenerate this from [TransactionDto] — see the class doc.
         */
        const val V1_BACKUP = """
{
    "schemaVersion": 1,
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
            "date": 1748000000000,
            "accountId": "acc-1",
            "categoryId": "cat-income"
        },
        {
            "transactionId": "tx-2",
            "type": "Spend",
            "amountCents": 8540,
            "description": "Mercado",
            "date": 1747976400000,
            "accountId": "acc-1",
            "categoryId": "cat-spend"
        },
        {
            "transactionId": "tx-3",
            "type": "Spend",
            "amountCents": 1200,
            "description": "Café",
            "date": 1748100000000,
            "accountId": "acc-1",
            "categoryId": null
        }
    ]
}
"""
    }
}
