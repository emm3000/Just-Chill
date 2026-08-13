package com.emm.data.backup

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.CategoryRepository
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
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A backup file written by the app at schema version 2 must keep restoring, forever — and it must
 * not take the recurring movements already on the device with it.
 *
 * Version 2 is the format every file the author has on disk today was written in. Those files sit in
 * storage the user chose, outside the app, where no schema migration can ever reach them. Once
 * `BACKUP_SCHEMA_VERSION` moves to 3, `decodePayload` — which dispatches on the version the file
 * *declares* — stops having any branch that matches them, and refuses an intact backup as
 * `BackupVersionUnsupported`. That is the failure this suite exists to prevent, and the reason
 * `BackupV2.kt` freezes the v2 payload shape ahead of the bump.
 *
 * ### Which half of this suite proves what, today
 *
 * `BACKUP_SCHEMA_VERSION` is still 2, so the two paths are not yet distinguishable at runtime:
 *
 *  - **The two `decodes`/`toCurrent` tests exercise the frozen reader directly**, by naming
 *    [ExportPayloadV2Dto] rather than going through the repository. They are the only assertions
 *    here that actually run frozen code, and they keep working unchanged after the bump.
 *  - **Every restore test below still reaches the database through the CURRENT branch**, because a
 *    file declaring 2 matches `BACKUP_SCHEMA_VERSION` today. They pin the behaviour a v2 file must
 *    produce, not the reader that produces it. When the next commit adds `recurringMovements`, moves
 *    the number and wires the v2 branch, these same assertions re-point onto the frozen reader with
 *    no edit — and the recurring-movement guard is the one that has to stay green through it.
 *
 * **The fixture below is written out by hand and must never be generated from a DTO.** A fixture
 * built from the current types is the test that stays green straight through the breaking change it
 * was supposed to catch: rename the field and the fixture renames itself. These are literal bytes,
 * frozen. If a change makes this suite fail, the change breaks real files on a real disk — fix the
 * reader, never the fixture.
 */
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
            transactions = mockk<TransactionRepository> { every { all() } returns flowOf(emptyList()) },
            categories = mockk<CategoryRepository> { every { all() } returns flowOf(emptyList()) },
            accounts = mockk<AccountRepository> { every { all() } returns flowOf(emptyList<Account>()) },
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

    // ── the frozen reader, exercised directly ─────────────────────────────────
    // These two name ExportPayloadV2Dto rather than going through importFromJson, because the
    // repository cannot reach it yet — BACKUP_SCHEMA_VERSION is still 2, so a v2 file matches the
    // current branch. Until the bump they are the only proof the frozen shape parses real v2 bytes
    // at all, and a frozen type nothing exercises is a frozen type that rots.

    @Test
    fun `the frozen version 2 shape parses the bytes a version 2 export actually wrote`() {
        val payload = importJson.decodeFromString<ExportPayloadV2Dto>(V2_BACKUP)

        assertEquals(BACKUP_SCHEMA_VERSION_V2, payload.schemaVersion)
        assertEquals(1_748_000_000_000L, payload.exportedAt)
        assertEquals("2.4.0", payload.appVersion)
        assertEquals(listOf("acc-1"), payload.accounts.map { it.accountId })
        assertEquals(listOf("cat-income", "cat-spend"), payload.categories.map { it.categoryId })
        assertEquals(listOf("tx-1", "tx-2", "tx-3"), payload.transactions.map { it.transactionId })
        // The field that defines version 2: local text, never epoch millis.
        assertEquals("2025-05-23T06:33:20", payload.transactions[0].occurredAt)
        assertNull(payload.transactions[2].categoryId)
        // Written by `encodeDefaults`, so a real v2 file carries it even though it is a default.
        assertEquals("PEN", payload.accounts[0].currency)
    }

    @Test
    fun `toCurrent hands every field of a version 2 payload through unchanged`() {
        val frozen = importJson.decodeFromString<ExportPayloadV2Dto>(V2_BACKUP)

        val current = frozen.toCurrent()

        // The version is restamped to the current one — the payload has been converted, so it must
        // not keep claiming to be the shape it arrived as.
        assertEquals(BACKUP_SCHEMA_VERSION, current.schemaVersion)
        assertEquals(frozen.exportedAt, current.exportedAt)
        assertEquals(frozen.appVersion, current.appVersion)
        // Identity, not just equality of size: a mapping that dropped or reordered rows would still
        // pass a count check, and this conversion is meant to be lossless.
        assertEquals(frozen.accounts, current.accounts)
        assertEquals(frozen.categories, current.categories)
        assertEquals(frozen.transactions, current.transactions)
    }

    // ── the whole restore ─────────────────────────────────────────────────────
    // Still through the current branch today; see the class doc.

    @Test
    fun `a version 2 file still restores everything it carries`() = runTest {
        val stats = repository.importFromJson(V2_BACKUP)

        assertEquals(ImportStats(accounts = 1, categories = 2, transactions = 3), stats)
        assertEquals(1, db.accountsQueries.all().executeAsList().size)
        assertEquals(2, db.categoriesQueries.all().executeAsList().size)
        assertEquals(3, db.transactionsQueries.all().executeAsList().size)
    }

    @Test
    fun `a version 2 occurrence is stored as the same local text the file carried`() = runTest {
        repository.importFromJson(V2_BACKUP)

        // Version 2 is where `occurredAt` stopped being an instant, so there is no offset to apply
        // and none must be: the bytes in the column are the bytes in the file.
        val tx = db.transactionsQueries.find("tx-1").executeAsOne()
        assertEquals("2025-05-23T06:33:20", tx.occurredAt)
        assertEquals(450_000L, tx.amount)
        assertEquals("Sueldo mayo", tx.description)
        assertEquals("cat-income", tx.categoryId)
    }

    @Test
    fun `a version 2 midnight stays at midnight rather than sliding a day`() = runTest {
        repository.importFromJson(V2_BACKUP)

        // Local midnight is the value that moves visibly if a reader ever puts this text back
        // through a timezone: under Lima's offset it would land on the 22nd, a day before the one
        // the user wrote it on. Pinning it is what says the text is carried, not converted.
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

    /**
     * The assertion no older suite could make, and the one with the real teeth.
     *
     * Version 1 predates recurring movements entirely, so `BackupV1CompatibilityTest` cannot say
     * anything about them. Version 2 is the last format that does not carry them while the table is
     * fully populated on the author's device — which makes a v2 import the exact moment where a
     * v3-shaped sweep would destroy data no backup on disk can put back.
     *
     * The rule this pins: **a file older than version 3 leaves `recurring_movements` alone.** Not
     * "restores them" — it has none to restore — but leaves the rows that are already there live,
     * untombstoned, and still active.
     */
    @Test
    fun `importing a version 2 file leaves the recurring movements already on the device alive`() = runTest {
        // The account and category the template hangs off have to exist before it does: the FK to
        // accounts is real, and so is the composite (categoryId, type) key into categories.
        repository.importFromJson(V2_BACKUP)
        exec(
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, " +
                "accountId, dayOfMonth, createdAt, updatedAt) " +
                "VALUES ('rec-1', 'Alquiler', 'Spend', 120000, '', 'cat-spend', 'acc-1', 5, 1, 1)",
        )

        repository.importFromJson(V2_BACKUP)

        // The two destructive shapes first, through raw SQL that sees every row — `find` filters
        // `deletedAt IS NULL`, so it cannot tell "physically deleted" from "tombstoned", and asking
        // it for `deletedAt` would be vacuous since it can never return a row where that is set.
        // Asserting these before the read is what makes each mode fail with its own message instead
        // of both arriving as an NPE out of `executeAsOne`.
        assertEquals(1, rawCount("SELECT COUNT(*) FROM recurring_movements"), "the row was deleted outright")
        assertEquals(
            0,
            rawCount("SELECT COUNT(*) FROM recurring_movements WHERE deletedAt IS NOT NULL"),
            "the row was tombstoned by the import's sweep",
        )
        // ...and it is still the template it was, readable through the live query.
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
         * Configured like `DefaultBackupRepository`'s own private `importJson`, so the direct-decode
         * tests read the fixture under the same leniency the real import path uses.
         */
        val importJson = Json { ignoreUnknownKeys = true }

        /**
         * A real version-2 export, byte for byte: `"occurredAt"` as ISO local text, no `"date"` and
         * no `"recurringMovements"` anywhere. Do not regenerate this from [ExportPayloadDto] — see
         * the class doc.
         */
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
