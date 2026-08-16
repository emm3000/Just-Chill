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

/**
 * The EXPORT half, against a real in-memory SQLDelight database.
 *
 * **This suite used to mock the four repository interfaces the export read through.** It no longer
 * can: the export is one `transactionWithResult` over direct queries, so the only collaborator left
 * is the database itself. That is a straight upgrade rather than a cost — the mocks described a
 * world in which `all()` returned whatever a test handed it, so nothing here could observe that the
 * export reads the tombstone filter, or the `ORDER BY`, or the statement that keeps paused
 * templates. Every one of those is now pinned below, and two of them (the ordering, and the
 * dangling category reached by an actual soft-delete) were simply unreachable before.
 *
 * FK enforcement is ON, matching the driver callback the app runs with and the other two backup
 * suites. It is what makes the "category is gone" fixtures honest: a movement can only point at a
 * category that once existed, so the test has to tombstone one rather than invent an id.
 */
class DefaultBackupRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var repository: DefaultBackupRepository

    // Export takes its `exportedAt` from the caller, so nothing in this suite reads the clock —
    // but it is stated rather than left to the machine all the same. What an import stamps is
    // asserted in DefaultBackupRepositoryImportTest, which reads it back from the same database.
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
        // The literal, deliberately, and not BACKUP_SCHEMA_VERSION: asserting the constant against
        // itself only says `encodeDefaults` works. This number is the one written into files other
        // versions of the app have to read, so bumping it must cost a deliberate edit here.
        assertEquals(3, payload.schemaVersion)
        assertEquals(1_748_000_000_000L, payload.exportedAt)
        assertEquals("1.0.0", payload.appVersion)
        assertEquals(1, payload.accounts.size)
        assertEquals("acc-1", payload.accounts[0].accountId)
        assertEquals("Yape", payload.accounts[0].name)
        assertEquals(setOf("cat-1", "cat-2"), payload.categories.mapTo(mutableSetOf()) { it.categoryId })
        // `transactions.all:` is `ORDER BY occurredAt DESC`, so the file carries the newest movement
        // first. The mocked suite this replaced returned whatever list it was handed and could not
        // say what the app actually writes.
        assertEquals(listOf("tx-2", "tx-1"), payload.transactions.map { it.transactionId })
        assertEquals(150_00L, payload.transactions[0].amountCents)
        assertEquals(4500_00L, payload.transactions[1].amountCents)
    }

    @Test
    fun `a categoryId whose category is gone is exported as null`() = runTest {
        // Deleting a category no longer nulls the column on its movements, so a live transaction
        // can point at a tombstoned category. Tombstoned categories are not exported, so leaving
        // the id in the file would produce a backup that fails its own import on the FK.
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

    /**
     * A backup is of what the device HOLDS, and a tombstone is not held any more.
     *
     * Newly reachable, and the reason it is worth its lines: the four statements the export reads
     * used to be picked by the four `LocalDataSource` classes, and this suite mocked past them
     * entirely. Now the export names them itself, so choosing one without `deletedAt IS NULL` —
     * or `selectActive:` in place of `selectAllLive:` — is a one-word edit with nothing else in the
     * repo to catch it.
     */
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

    // ── recurring movements: what version 3 added ─────────────────────────────
    //
    // This suite is the EXPORT half and only that. The two data-loss guards that used to sit in this
    // section — `lastConfirmedPeriod` and `createdAt`, the two fields that lose data in opposite
    // directions — moved to `DefaultBackupRepositoryImportTest` when the restore mapper landed. They
    // run the full round trip there, which is the only place the claim their names make can actually
    // be held.

    /**
     * A paused template is data the user still owns, so the export reads every LIVE row — not every
     * ACTIVE one.
     *
     * `recurring_movements.sq` carries three all-row reads and only one of them fits: `selectActive:`
     * filters `isActive = 1`, which would drop this test's second template on the floor, and
     * `selectAllWithDetails:` is a joined shape carrying three columns the format does not want.
     * `selectAllLive:` is the third, added for the export.
     */
    @Test
    fun `the export carries every live template, the paused ones included`() = runTest {
        insertAccount()
        insertCategory(categoryId = "cat-2", name = "Comida", categoryType = "Spend")
        insertTemplate(id = "rec-1", name = "Alquiler", categoryId = "cat-2")
        insertTemplate(id = "rec-2", name = "Gimnasio", categoryId = "cat-2", isActive = 0L)

        val payload = exportedPayload()

        assertEquals(listOf("rec-1", "rec-2"), payload.recurringMovements.map { it.recurringMovementId })
        assertEquals(listOf(true, false), payload.recurringMovements.map { it.isActive })
        // The rest of the shape, once: a template is twelve columns and a file that carries the id
        // and loses the amount is not a backup of anything.
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
        // The column is nullable and the two readings are different money: "no amount agreed yet"
        // is not "an alquiler of S/ 0.00", and a restore that turns one into the other writes a
        // number the user never entered.
        insertAccount()
        insertTemplate(id = "rec-1", amount = null)

        assertNull(exportedPayload().recurringMovements.single().amountCents)
    }

    /**
     * The two fields that are behaviour rather than storage reach the file at all.
     *
     * Shape only, and deliberately so: what each of them costs when a restore loses it — a re-minted
     * month, or every owed month swallowed — is held by the round-trip guards in
     * `DefaultBackupRepositoryImportTest`, which can run the restored values back through the real
     * `pendingPeriods` rule. A `createdAt` this suite could assert against the export alone is a
     * `createdAt` the import is still free to overwrite.
     */
    @Test
    fun `the export carries a template's own settled mark and createdAt`() = runTest {
        insertAccount()
        insertTemplate(id = "rec-1", lastConfirmedPeriod = "2026-07", createdAt = TEMPLATE_CREATED)

        val exported = exportedPayload(exportedAt = EXPORTED_AT).recurringMovements.single()

        assertEquals("2026-07", exported.lastConfirmedPeriod)
        // The export's own instant is August; the template's is June, and it is the template's that
        // has to travel.
        assertEquals(TEMPLATE_CREATED, exported.createdAt)
    }

    @Test
    fun `a template whose category is gone is exported with no category`() = runTest {
        // Same hazard as the transaction above, through the same composite key: `recurring_movements`
        // carries (categoryId, type) -> categories(categoryId, categoryType). Exporting an id whose
        // category is tombstoned — and therefore not in the file — writes a backup that fails its
        // own import on the FK.
        insertAccount()
        insertCategory(categoryId = "cat-1", name = "Comida", categoryType = "Spend")
        insertCategory(categoryId = "cat-deleted", name = "Antojos", categoryType = "Spend")
        insertTemplate(id = "rec-1", categoryId = "cat-deleted")
        insertTemplate(id = "rec-2", name = "Bus", categoryId = "cat-1")
        db.categoriesQueries.softDelete(deletedAt = 1L, updatedAt = 1L, categoryId = "cat-deleted")

        val exported = exportedPayload().recurringMovements

        assertNull(exported.single { it.recurringMovementId == "rec-1" }.categoryId)
        // ...and the live one keeps its label: a scrub that stripped every id would also pass above.
        assertEquals("cat-1", exported.single { it.recurringMovementId == "rec-2" }.categoryId)
    }

    // ── the two properties the transactional refactor introduced ──────────────

    /**
     * **A database failure leaves the export as a [DomainException], never as a raw SQLite throw.**
     *
     * Reading [EmmDatabaseData] directly took `catchAsDomainException()` off this path — it lived on
     * the four repository interfaces the export no longer holds — so the translation had to be put
     * back explicitly, with `safeDbCall`. Nothing else in the repo would have noticed it missing:
     * `ProfileViewModel` would have shown the generic unknown failure and the file would simply not
     * have been written.
     *
     * The assertion is the FAMILY, not `DatabaseError`, and that is not slack. `isSqliteException()`
     * is an `expect fun` whose Android actual tests for `android.database.sqlite.SQLiteException`,
     * which the JVM host driver never throws — so on this source set the same failure that is a
     * `DatabaseError` on a device arrives as `DomainException.Unknown`. Pinning the subtype here
     * would pin the host's accident. What matters, and what is asserted, is that nothing raw escapes.
     */
    @Test
    fun `a database failure during the export surfaces as a DomainException`() = runTest {
        // No table references `recurring_movements`, so it can be dropped outright — and the export
        // reads it third, after two statements that succeed.
        driver.execute(null, "DROP TABLE recurring_movements", 0)

        assertFailsWith<DomainException> {
            repository.exportToJson(exportedAt = 0L, appVersion = "1.0.0")
        }
    }

    /**
     * **A serialization failure surfaces as its own named reason, not as [DomainException.Unknown].**
     *
     * `exportToJson` used to hand `exportJson.encodeToString(payload)` to nothing — only
     * `safeDbCall` wrapped the DB read above it, so an encode failure escaped as a raw
     * [SerializationException], caught only by the generic branch in `BackupOrchestrator.runBackup`
     * and reported with no reason a log or the UI could name. ADR 009 Phase 3 asks every backup
     * failure mode, serialization included, to log a distinct reason.
     *
     * [encodeAsDomainException] is exercised directly rather than through `exportToJson`: every
     * field [ExportPayloadDto] carries is a plain `String`/`Long`/`Boolean`/`List`, so there is no
     * well-formed payload that can make the real encoder throw — see that function's own KDoc.
     */
    @Test
    fun `a serialization failure translates to SerializationError, not Unknown or DatabaseError`() {
        val ex = assertFailsWith<DomainException.SerializationError> {
            encodeAsDomainException<String> { throw SerializationException("boom") }
        }

        assertEquals("boom", ex.message)
    }

    /**
     * **Every read the export makes happens inside one open transaction, and there are exactly four.**
     *
     * *What this proves and what it does not.* It does NOT prove atomicity against a concurrent
     * writer, and no host test here can: `JdbcSqliteDriver(IN_MEMORY)` is a single connection, so
     * there is no second connection for a competing write to arrive on and nothing to interleave.
     * Isolation is SQLite's property, not this code's, and it only has anything to isolate once the
     * reads are inside a transaction at all.
     *
     * *That* is the property this pins, and it is the exact one the refactor added — the four
     * `Flow.first()` calls it replaced each opened and closed their own implicit transaction, so a
     * write landing between two of them was visible to the second and not the first. The spy records
     * `currentTransaction()` at the moment of every `executeQuery`, so a revert to per-read
     * statements shows up as `false` entries; the count of four shows up if a read is dropped, or if
     * one migrates into a per-row loop.
     */
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

    /**
     * Records, for each query it forwards, whether a SQLDelight transaction was open at the time.
     *
     * Delegation rather than a hand-written implementation: the point is to observe one method and
     * change nothing, and `newTransaction()`/`currentTransaction()` must keep landing on the real
     * driver or the reading would be of the spy's own bookkeeping.
     */
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

    // ── helpers ───────────────────────────────────────────────────────────────

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
        /** 2026-06-10 — the template's own instant, and two months before the export's. */
        val TEMPLATE_CREATED: Long = Instant.parse("2026-06-10T12:00:00Z").toEpochMilliseconds()

        /**
         * August, deliberately — it is the value a `createdAt = now` restore would stamp, so a file
         * that lost the field would still look plausible while owing nothing before this month.
         */
        val EXPORTED_AT: Long = Instant.parse("2026-08-11T15:04:05Z").toEpochMilliseconds()
    }
}
