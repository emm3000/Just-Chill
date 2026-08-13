package com.emm.data.backup

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.data.transaction.asEntity
import com.emm.data.transaction.asExternalModel
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
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

// Uses an in-memory JVM SQLite driver (app.cash.sqldelight:sqlite-driver) to exercise the full
// SQLDelight schema — the atomicity of the transaction block and the actual queries.
//
// FK enforcement is switched ON here via PRAGMA, matching the Android driver callback. It used to
// be off, which is why the import path could physically DELETE parent rows for years without any
// test noticing that ON DELETE RESTRICT would reject it on a real device.
class DefaultBackupRepositoryImportTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: EmmDatabaseData

    private val transactionRepo = mockk<TransactionRepository> {
        every { all() } returns flowOf(emptyList())
    }
    private val categoryRepo = mockk<CategoryRepository> {
        every { all() } returns flowOf(emptyList())
    }
    private val accountRepo = mockk<AccountRepository> {
        every { all() } returns flowOf(emptyList<Account>())
    }

    private lateinit var repository: DefaultBackupRepository

    /**
     * Returns a DIFFERENT instant on every read — the current one, then a millisecond later.
     *
     * A clock that returns a constant cannot tell "read once" from "read per row": move the read
     * inside the restore loop and every assertion still passes, because every read answers the
     * same. Ticking makes the two observable, which is the only way a test can hold #6's
     * once-per-write rule rather than just the weaker "the clock is injected".
     *
     * [instant] is settable so a test can put the next import at a chosen time instead of
     * whatever the ticks have reached.
     */
    private class TickingClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant.also { instant += 1.milliseconds }
    }

    private val clock = TickingClock(FIRST_IMPORT)

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        exec("PRAGMA foreign_keys=ON")
        clock.instant = FIRST_IMPORT
        repository = DefaultBackupRepository(
            transactions = transactionRepo,
            categories = categoryRepo,
            accounts = accountRepo,
            db = db,
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `valid payload returns correct ImportStats`() = runTest {
        val json = buildPayloadJson(accounts = 1, categories = 2, transactions = 3)

        val stats: ImportStats = repository.importFromJson(json)

        assertEquals(ImportStats(accounts = 1, categories = 2, transactions = 3), stats)
    }

    @Test
    fun `valid payload inserts all rows into the DB`() = runTest {
        val json = buildPayloadJson(accounts = 2, categories = 3, transactions = 4)

        repository.importFromJson(json)

        val accountCount = db.accountsQueries.all().executeAsList().size
        val categoryCount = db.categoriesQueries.all().executeAsList().size
        val txCount = db.transactionsQueries.all().executeAsList().size
        assertEquals(2, accountCount)
        assertEquals(3, categoryCount)
        assertEquals(4, txCount)
    }

    @Test
    fun `importing twice leaves only the rows of the second backup live`() = runTest {
        val firstJson = buildPayloadJson(accounts = 2, categories = 3, transactions = 4)
        val secondJson = buildPayloadJson(accounts = 1, categories = 1, transactions = 1)

        repository.importFromJson(firstJson)
        repository.importFromJson(secondJson)

        val accountCount = db.accountsQueries.all().executeAsList().size
        val categoryCount = db.categoriesQueries.all().executeAsList().size
        val txCount = db.transactionsQueries.all().executeAsList().size
        assertEquals(1, accountCount)
        assertEquals(1, categoryCount)
        assertEquals(1, txCount)
    }

    @Test
    fun `empty payload succeeds with ImportStats zeros and wipes the DB`() = runTest {
        // Seed some data first
        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        val stats = repository.importFromJson(EMPTY_PAYLOAD_JSON)

        assertEquals(ImportStats(accounts = 0, categories = 0, transactions = 0), stats)
        assertTrue(db.accountsQueries.all().executeAsList().isEmpty())
        assertTrue(db.categoriesQueries.all().executeAsList().isEmpty())
        assertTrue(db.transactionsQueries.all().executeAsList().isEmpty())
    }

    @Test
    fun `corrupt JSON throws ValidationError and DB is untouched`() = runTest {
        val beforeAccounts = db.accountsQueries.all().executeAsList().size

        assertFailsWith<DomainException.ValidationError> {
            repository.importFromJson("{ not valid json at all")
        }

        assertEquals(beforeAccounts, db.accountsQueries.all().executeAsList().size)
    }

    @Test
    fun `wrong schemaVersion throws ValidationError and DB is untouched`() = runTest {
        val json = """{"schemaVersion":99,"exportedAt":0,"appVersion":"1.0.0",""" +
            """"accounts":[],"categories":[],"transactions":[]}"""
        val beforeAccounts = db.accountsQueries.all().executeAsList().size

        assertFailsWith<DomainException.ValidationError> {
            repository.importFromJson(json)
        }

        assertEquals(beforeAccounts, db.accountsQueries.all().executeAsList().size)
    }

    // ── an untrusted file: rows the app cannot interpret ──────────────────────

    /**
     * The file is plain JSON in storage the user chose. It can have been hand-edited, truncated, or
     * written by something else — so a row in it is not automatically a row this app can hold.
     *
     * Writing one anyway is the worst of the available options, and it is what used to happen: the
     * column is NOT NULL so the INSERT succeeds, but every mapper on the read path drops the row.
     * The movement is then invisible on every screen AND counted in the balance, which is precisely
     * the "the total disagrees with the list" defect the whole occurredAt model exists to remove.
     */
    @Test
    fun `a movement the app cannot read is not restored, and the balance still matches the ledger`() = runTest {
        val json = payloadWithTransactions(
            """{"transactionId":"tx-ok","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":null}""",
            // A hand-edited date: one-digit month and day, a space instead of the 'T'. No writer in
            // the app can produce it, and nothing on the read path can parse it.
            """{"transactionId":"tx-broken","type":"Income","amountCents":777700,"description":"Roto",""" +
                """"occurredAt":"2026-8-1 12:00","accountId":"acc-1","categoryId":null}""",
        )

        repository.importFromJson(json)

        assertEquals(0, rawCount("SELECT COUNT(*) FROM transactions WHERE transactionId = 'tx-broken'"))
        // The two numbers Home puts next to each other: the balance aggregate reads the column
        // directly, the ledger goes through the mappers. They must fold the same rows.
        val balance = db.transactionsQueries.liveTotals().executeAsOne().balance
        val visible = db.transactionsQueries.all().executeAsList().asEntity().asExternalModel()
        assertEquals(visible.sumOf { it.amount.cents }, balance)
        assertEquals(10_000L, balance)
    }

    @Test
    fun `the reported count is what landed, not what the file held`() = runTest {
        val json = payloadWithTransactions(
            """{"transactionId":"tx-ok","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":null}""",
            """{"transactionId":"tx-broken","type":"Income","amountCents":7777,"description":"Roto",""" +
                """"occurredAt":"not a date at all","accountId":"acc-1","categoryId":null}""",
        )

        val stats = repository.importFromJson(json)

        // "2 movimientos importados" for a file whose second movement is nowhere is a lie the user
        // has no way to check — and the count is the only feedback the import gives.
        assertEquals(1, stats.transactions)
    }

    @Test
    fun `a movement with a type the app does not know is dropped rather than failing the import`() = runTest {
        val json = payloadWithTransactions(
            """{"transactionId":"tx-ok","type":"Spend","amountCents":5000,"description":"Mercado",""" +
                """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":null}""",
            """{"transactionId":"tx-weird","type":"Transfer","amountCents":5000,"description":"?",""" +
                """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":null}""",
        )

        val stats = repository.importFromJson(json)

        // Same skip-the-row policy the storage mappers apply. One uninterpretable movement costs
        // one movement — never the whole restore, and never a file reported as corrupt.
        assertEquals(1, stats.transactions)
        assertEquals(1, db.transactionsQueries.all().executeAsList().size)
    }

    @Test
    fun `a movement written without seconds restores in the canonical shape`() = runTest {
        // The lenient parser accepts it, so it must not be dropped — but it is re-encoded on the
        // way in, so two writes of the same moment cannot end up as two different strings.
        val json = payloadWithTransactions(
            """{"transactionId":"tx-short","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                """"occurredAt":"2026-05-23T09:33","accountId":"acc-1","categoryId":null}""",
        )

        repository.importFromJson(json)

        assertEquals("2026-05-23T09:33:00", db.transactionsQueries.find("tx-short").executeAsOne().occurredAt)
    }

    // ── a file older than the composite key ───────────────────────────────────

    /**
     * **Every backup file that exists today predates the composite key**, and the export that wrote
     * them copied whatever pair the app had stored — mismatches included, since nothing checked.
     *
     * Under the key those rows no longer insert. The whole restore runs inside ONE transaction, so
     * a raw constraint violation does not cost one movement: it rolls the import back and leaves
     * the owner with the tombstone sweep and nothing else. That is the only safety net on a device
     * holding real accumulated data, which is why the row lands uncategorized instead.
     */
    @Test
    fun `a movement filed under a category of the other type restores uncategorized`() = runTest {
        val json = payloadWith(
            categoriesJson = listOf(
                """{"categoryId":"cat-spend","name":"Café","icon":"i","color":"c","categoryType":"Spend"}""",
            ),
            transactionsJson = listOf(
                """{"transactionId":"tx-mismatch","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                    """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":"cat-spend"}""",
            ),
        )

        val stats = repository.importFromJson(json)

        // The movement, its amount and its type are the data. The category is a label.
        assertEquals(1, stats.transactions)
        val tx = db.transactionsQueries.find("tx-mismatch").executeAsOne()
        assertNull(tx.categoryId)
        assertEquals("Income", tx.type)
        assertEquals(10_000L, tx.amount)
    }

    @Test
    fun `one mismatched movement does not cost the rest of the file`() = runTest {
        // The assertion that says a stale pair is a lost label and not a lost backup: without the
        // repair the constraint violation aborts the enclosing transaction and tx-ok never lands.
        val json = payloadWith(
            categoriesJson = listOf(
                """{"categoryId":"cat-income","name":"Sueldo","icon":"i","color":"c","categoryType":"Income"}""",
            ),
            transactionsJson = listOf(
                """{"transactionId":"tx-mismatch","type":"Spend","amountCents":500,"description":"Café",""" +
                    """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":"cat-income"}""",
                """{"transactionId":"tx-ok","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                    """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":"cat-income"}""",
            ),
        )

        val stats = repository.importFromJson(json)

        assertEquals(2, stats.transactions)
        assertNull(db.transactionsQueries.find("tx-mismatch").executeAsOne().categoryId)
        // ...and the matching pair keeps its category: a repair that stripped every id would also
        // have passed the test above.
        assertEquals("cat-income", db.transactionsQueries.find("tx-ok").executeAsOne().categoryId)
    }

    @Test
    fun `a movement pointing at a category the file never carried restores uncategorized`() = runTest {
        // The export drops dangling ids, so this shape only reaches the app from a hand-edited or
        // truncated file — where it used to abort the entire restore on the FK.
        val json = payloadWith(
            categoriesJson = emptyList(),
            transactionsJson = listOf(
                """{"transactionId":"tx-orphan","type":"Spend","amountCents":500,"description":"Café",""" +
                    """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":"cat-gone"}""",
            ),
        )

        val stats = repository.importFromJson(json)

        assertEquals(1, stats.transactions)
        assertNull(db.transactionsQueries.find("tx-orphan").executeAsOne().categoryId)
    }

    /**
     * The parent half of the same problem, and the one the export cannot protect against.
     *
     * A recurring movement holds `(cat-1, Spend)`. The file redefines `cat-1` as an Income
     * category. Import never touches `recurring_movements` — deliberately, since the format does
     * not carry them — so the movement still holds the old pair when the category's type is
     * rewritten, and SQLite refuses the parent-key change. Inside the single wrapping transaction
     * that is not one failed row: it is the whole restore rolled back, after the tombstone sweep.
     */
    @Test
    fun `a backup that redefines a category's type does not abort on the movements filed under it`() = runTest {
        repository.importFromJson(
            payloadWith(
                categoriesJson = listOf(
                    """{"categoryId":"cat-1","name":"Bar","icon":"i","color":"c","categoryType":"Spend"}""",
                ),
                transactionsJson = emptyList(),
            ),
        )
        exec(
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, " +
                "accountId, dayOfMonth, createdAt, updatedAt) " +
                "VALUES ('rec-1', 'Alquiler', 'Spend', 5000, '', 'cat-1', 'acc-1', 5, 1, 1)",
        )

        val stats = repository.importFromJson(
            payloadWith(
                categoriesJson = listOf(
                    """{"categoryId":"cat-1","name":"Sueldo","icon":"i","color":"c","categoryType":"Income"}""",
                ),
                transactionsJson = listOf(
                    """{"transactionId":"tx-1","type":"Income","amountCents":10000,"description":"Sueldo",""" +
                        """"occurredAt":"2026-05-23T09:33:20","accountId":"acc-1","categoryId":"cat-1"}""",
                ),
            ),
        )

        assertEquals(1, stats.transactions, "the import must complete, not roll back")
        assertEquals("Income", db.categoriesQueries.find("cat-1").executeAsOne().categoryType)
        assertEquals("cat-1", db.transactionsQueries.find("tx-1").executeAsOne().categoryId)
        // The template survives, minus a label it can no longer hold. It is never deleted: the
        // import does not own that table.
        assertEquals(1, rawCount("SELECT COUNT(*) FROM recurring_movements WHERE id = 'rec-1'"))
        assertNull(db.recurring_movementsQueries.find("rec-1").executeAsOne().categoryId)
    }

    // ── the version probe ─────────────────────────────────────────────────────

    @Test
    fun `a file with no schemaVersion reads as version 1 rather than as an unreadable version`() = runTest {
        // The key only started being written when there was a second version to tell apart, so its
        // absence means "old file", not "from the future". Saying otherwise blames the wrong thing.
        val json = """{"exportedAt":0,"appVersion":"2.4.0","accounts":[""" +
            """{"accountId":"acc-1","name":"BCP","type":"Bank","currency":"PEN"}],"categories":[],""" +
            """"transactions":[{"transactionId":"tx-1","type":"Income","amountCents":450000,""" +
            """"description":"Sueldo","date":1748000000000,"accountId":"acc-1","categoryId":null}]}"""

        val stats = repository.importFromJson(json)

        assertEquals(1, stats.transactions)
        assertEquals("2025-05-23T06:33:20", db.transactionsQueries.find("tx-1").executeAsOne().occurredAt)
    }

    @Test
    fun `a file whose root is not an object is reported as corrupt, not as a raw crash`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            repository.importFromJson("""[{"schemaVersion":2}]""")
        }

        assertEquals(ValidationCode.BackupFileInvalid, ex.code)
    }

    @Test
    fun `a schemaVersion that is not a number is reported as corrupt, not as an unsupported version`() = runTest {
        // It used to escape the guard entirely: the probe ran outside it, so the accessor's own
        // throw reached the caller and the user got the generic failure instead of this one.
        listOf("""{"schemaVersion":{}}""", """{"schemaVersion":[]}""", """{"schemaVersion":"dos"}""").forEach { root ->
            val ex = assertFailsWith<DomainException.ValidationError>("root was $root") {
                repository.importFromJson(root)
            }
            assertEquals(ValidationCode.BackupFileInvalid, ex.code, "root was $root")
        }
    }

    // ── the stamp an import writes ────────────────────────────────────────────

    /**
     * Every row an import restores carries the SAME stamp, and it is the first tick of the
     * injected clock — so the clock is read once for the whole import, not once per row.
     *
     * Both halves matter and the second is the one with teeth. `docs/DATE_AUDIT.md` #6 states that
     * `:data` reads an injected `Clock` **once per write**; every other writer in the module already
     * did, and this one was the exception, on the single path that rewrites every row the user owns
     * at once. A constant fake clock cannot hold that rule — it answers the same however often it is
     * asked — so [TickingClock] moves a millisecond per read and the equality below is what fails if
     * the read migrates into the restore loop.
     *
     * The whole assertion was impossible before: the repository read `Clock.System` directly, so the
     * timestamps an import produced were whatever the machine said while the suite ran.
     */
    @Test
    fun `an import reads the clock once and stamps every restored row with it`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        // The FIRST tick, and the only one: three tables, six columns, one value.
        val expected = FIRST_IMPORT.toEpochMilliseconds()
        assertEquals(expected, rawStamp("SELECT createdAt FROM accounts WHERE accountId = 'acc-1'"))
        assertEquals(expected, rawStamp("SELECT updatedAt FROM accounts WHERE accountId = 'acc-1'"))
        assertEquals(expected, rawStamp("SELECT createdAt FROM categories WHERE categoryId = 'cat-1'"))
        assertEquals(expected, rawStamp("SELECT updatedAt FROM categories WHERE categoryId = 'cat-1'"))
        assertEquals(expected, rawStamp("SELECT createdAt FROM transactions WHERE transactionId = 'tx-1'"))
        assertEquals(expected, rawStamp("SELECT updatedAt FROM transactions WHERE transactionId = 'tx-1'"))
    }

    /**
     * The tombstone an import leaves behind carries the same instant as the rows it restores.
     *
     * The other half of reading the clock once, across the tombstone/restore boundary rather than
     * across rows: the sweep and the restores are one atomic "make everything look like this file",
     * so a row the file dropped must not sort before or after a row it kept. Against a ticking
     * clock, a per-statement read would give them different values and this fails.
     *
     * It also pins what a re-import does NOT change — `createdAt` survives, because the insert is
     * `INSERT OR IGNORE` and only `restoreFromBackup` writes `updatedAt`.
     */
    @Test
    fun `a second import stamps its own instant, and the tombstone it leaves shares it`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 2, categories = 1, transactions = 1))
        clock.instant = SECOND_IMPORT

        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        val second = SECOND_IMPORT.toEpochMilliseconds()
        // acc-2 is missing from the second file: tombstoned, stamped at the second import.
        assertEquals(second, rawStamp("SELECT deletedAt FROM accounts WHERE accountId = 'acc-2'"))
        assertEquals(second, rawStamp("SELECT updatedAt FROM accounts WHERE accountId = 'acc-2'"))
        // acc-1 survives it and is re-stamped with the same instant, so the restore wins LWW.
        assertEquals(second, rawStamp("SELECT updatedAt FROM accounts WHERE accountId = 'acc-1'"))
        assertNull(rawStamp("SELECT deletedAt FROM accounts WHERE accountId = 'acc-1'"))
        // ...but keeps the createdAt of the import that first brought it in — INSERT OR IGNORE.
        assertEquals(
            FIRST_IMPORT.toEpochMilliseconds(),
            rawStamp("SELECT createdAt FROM accounts WHERE accountId = 'acc-1'"),
        )
    }

    // ── replace semantics: tombstones, not physical deletes ───────────────────

    @Test
    fun `rows missing from the backup are tombstoned so the deletion can sync`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 2, categories = 2, transactions = 2))

        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        // Still physically present, but tombstoned and queued for push.
        assertEquals(2, rawCount("SELECT COUNT(*) FROM accounts"))
        assertEquals(
            1,
            rawCount("SELECT COUNT(*) FROM accounts WHERE accountId = 'acc-2' AND deletedAt IS NOT NULL"),
        )
        assertEquals(
            1,
            rawCount("SELECT COUNT(*) FROM transactions WHERE transactionId = 'tx-2' AND syncState = 'Pending'"),
        )
    }

    @Test
    fun `a row that comes back in a later backup is resurrected`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))
        repository.importFromJson(EMPTY_PAYLOAD_JSON)
        assertEquals(1, rawCount("SELECT COUNT(*) FROM accounts WHERE deletedAt IS NOT NULL"))

        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        assertEquals(0, rawCount("SELECT COUNT(*) FROM accounts WHERE deletedAt IS NOT NULL"))
        assertEquals(1, db.accountsQueries.all().executeAsList().size)
        assertEquals(1, db.transactionsQueries.all().executeAsList().size)
    }

    @Test
    fun `import does not break when a recurring movement references an account`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))
        exec(
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, " +
                "accountId, dayOfMonth, createdAt, updatedAt) " +
                "VALUES ('rec-1', 'Alquiler', 'Spend', 5000, '', 'cat-1', 'acc-1', 5, 1, 1)",
        )

        // A physical DELETE of acc-1 would be rejected here by ON DELETE RESTRICT.
        repository.importFromJson(EMPTY_PAYLOAD_JSON)

        assertEquals(1, rawCount("SELECT COUNT(*) FROM recurring_movements WHERE id = 'rec-1'"))
    }

    @Test
    fun `an already claimed row keeps its userId through an import`() = runTest {
        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))
        exec("UPDATE accounts SET userId = 'user-1', syncState = 'Synced'")

        repository.importFromJson(buildPayloadJson(accounts = 1, categories = 1, transactions = 1))

        assertEquals(1, rawCount("SELECT COUNT(*) FROM accounts WHERE userId = 'user-1'"))
        // Re-queued for push: the restore is the newer write and must win LWW.
        assertEquals(1, rawCount("SELECT COUNT(*) FROM accounts WHERE syncState = 'Pending'"))
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun rawCount(sql: String): Long = rawStamp(sql) ?: 0L

    /**
     * The first column of the first row as a NULLABLE Long — for reading a stamp back off a row.
     *
     * Nullable on purpose: `deletedAt` is null on a live row, and collapsing that to 0 would let
     * "this row was never tombstoned" pass an assertion about when it was.
     */
    private fun rawStamp(sql: String): Long? = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0))
        },
        parameters = 0,
    ).value

    /**
     * Builds a minimal valid JSON payload. Account IDs are sequential to ensure uniqueness.
     * Transaction accountId always references "acc-1" which is in the accounts list.
     *
     * **`schemaVersion` here must track [BACKUP_SCHEMA_VERSION].** This suite is what covers the
     * current reader branch; the per-version fixtures frozen in `BackupV1CompatibilityTest` and
     * `BackupV2CompatibilityTest` cover the old ones. Leaving a stale number here does not fail —
     * the old branch still restores it — it just quietly moves every test in this file onto a frozen
     * reader, which is how the current branch ends up with no coverage at all. The number is written
     * out rather than interpolated so the next bump has to look at these bytes.
     */
    private fun buildPayloadJson(accounts: Int, categories: Int, transactions: Int): String {
        val accountsJson = (1..accounts).joinToString(",") { i ->
            """{"accountId":"acc-$i","name":"Cuenta $i","type":"Cash","currency":"PEN"}"""
        }
        val categoriesJson = (1..categories).joinToString(",") { i ->
            """{"categoryId":"cat-$i","name":"Cat $i","icon":"icon","color":"#000","categoryType":"Spend"}"""
        }
        val transactionsJson = (1..transactions).joinToString(",") { i ->
            """{"transactionId":"tx-$i","type":"Spend","amountCents":1000,""" +
                """"description":"Tx $i","occurredAt":"2026-05-23T09:33:20",""" +
                """"accountId":"acc-1","categoryId":null}"""
        }
        return """
            {
                "schemaVersion": 2,
                "exportedAt": 0,
                "appVersion": "1.0.0",
                "accounts": [$accountsJson],
                "categories": [$categoriesJson],
                "transactions": [$transactionsJson]
            }
        """.trimIndent()
    }

    /** One account, no categories, and exactly the transaction JSON the test wrote by hand. */
    private fun payloadWithTransactions(vararg transactionsJson: String): String =
        payloadWith(categoriesJson = emptyList(), transactionsJson = transactionsJson.toList())

    /** One account, plus exactly the category and transaction JSON the test wrote by hand. */
    private fun payloadWith(categoriesJson: List<String>, transactionsJson: List<String>): String = """
        {
            "schemaVersion": 2,
            "exportedAt": 0,
            "appVersion": "1.0.0",
            "accounts": [{"accountId":"acc-1","name":"Cuenta","type":"Cash","currency":"PEN"}],
            "categories": [${categoriesJson.joinToString(",")}],
            "transactions": [${transactionsJson.joinToString(",")}]
        }
    """.trimIndent()

    private companion object {
        const val EMPTY_PAYLOAD_JSON = """{"schemaVersion":2,"exportedAt":0,"appVersion":"1.0.0",""" +
            """"accounts":[],"categories":[],"transactions":[]}"""

        // Two instants an hour apart. Stated, so the rows an import writes have a value to be
        // compared against rather than whatever the machine happened to read.
        val FIRST_IMPORT: Instant = Instant.parse("2026-08-11T15:04:05Z")
        val SECOND_IMPORT: Instant = Instant.parse("2026-08-11T16:04:05Z")
    }
}
