package com.emm.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Migration test: schema version 3 → 4 — `transactions.date INTEGER` (epoch millis) becomes
 * `transactions.occurredAt TEXT` (ISO local, no zone).
 *
 * **This is the repository's first DESTRUCTIVE migration.** SQLite cannot change a column's type,
 * so 3.sqm rebuilds the table: create, copy-converting, drop, rename, recreate the indexes. Every
 * step of that is a way to lose the whole table, and the instrumented suite is the only thing that
 * runs it against a real `AndroidSqliteDriver`.
 *
 * What is asserted, and why each one:
 *   1. a normal row converts to the expected ISO local string — the conversion itself;
 *   2. every row survives — a rebuild that drops the source table can silently lose all of them,
 *      and a count is the only assertion that notices;
 *   3. the indexes exist afterwards — they belong to the dropped table and must be recreated,
 *      and nothing else in the build would ever notice their absence;
 *   4. a `date` no `strftime` can read does NOT abort the migration — a NULL into the NOT NULL
 *      column would fail inside onUpgrade, leave user_version at 3, and fail identically on every
 *      subsequent open. That is a permanent open-crash, not a bad row.
 *
 * Migrates to `EmmDatabaseData.Schema.version`, never to a hardcoded 4 — the rule in
 * `data/CLAUDE.md`, written down after `MigrationV2ToV3Test` stopped at 3 and broke the moment a 4
 * existed. Every assertion below goes through generated queries, and those only ever match the
 * CURRENT schema; a run that stops mid-chain would be asserting against a shape it never reached.
 * It is also what a real device does: one open, the whole chain.
 *
 * Requires a device/emulator. Run with: `./gradlew :data:connectedAndroidDeviceTest`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationV3ToV4Test {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: EmmDatabaseData

    /**
     * The production schema at version 3 — all 4 tables exactly as they existed before this
     * change. DDL copied verbatim from the .sq files plus the 2.sqm sync-metadata columns.
     */
    private val schemaV3 = object : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 3

        // Four CREATE TABLEs and five indexes, verbatim. Splitting them up would hide the one
        // thing this block is for: being readable as the schema it claims to reproduce.
        @Suppress("LongMethod")
        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.execute(
                null,
                """
                CREATE TABLE accounts (
                    accountId  TEXT NOT NULL PRIMARY KEY,
                    name       TEXT NOT NULL,
                    type       TEXT NOT NULL DEFAULT 'Bank',
                    currency   TEXT NOT NULL DEFAULT 'PEN',
                    updatedAt  INTEGER NOT NULL,
                    createdAt  INTEGER NOT NULL,
                    userId     TEXT,
                    deletedAt  INTEGER,
                    syncState  TEXT NOT NULL DEFAULT 'Pending'
                );
                """.trimIndent(),
                0,
            )
            driver.execute(
                null,
                """
                CREATE TABLE categories (
                    categoryId    TEXT NOT NULL PRIMARY KEY,
                    name          TEXT NOT NULL,
                    icon          TEXT NOT NULL,
                    color         TEXT NOT NULL,
                    categoryType  TEXT NOT NULL,
                    isDefault     INTEGER NOT NULL DEFAULT 0,
                    updatedAt     INTEGER NOT NULL,
                    createdAt     INTEGER NOT NULL,
                    userId        TEXT,
                    deletedAt     INTEGER,
                    syncState     TEXT NOT NULL DEFAULT 'Pending'
                );
                """.trimIndent(),
                0,
            )
            driver.execute(
                null,
                """
                CREATE TABLE transactions (
                    transactionId  TEXT NOT NULL PRIMARY KEY,
                    type           TEXT NOT NULL,
                    amount         INTEGER NOT NULL,
                    description    TEXT NOT NULL DEFAULT '',
                    date           INTEGER NOT NULL,
                    categoryId     TEXT REFERENCES categories(categoryId) ON DELETE SET NULL,
                    accountId      TEXT NOT NULL REFERENCES accounts(accountId) ON DELETE RESTRICT,
                    createdAt      INTEGER NOT NULL,
                    updatedAt      INTEGER NOT NULL,
                    userId         TEXT,
                    deletedAt      INTEGER,
                    syncState      TEXT NOT NULL DEFAULT 'Pending'
                );
                """.trimIndent(),
                0,
            )
            driver.execute(
                null,
                """
                CREATE TABLE recurring_movements (
                    id                  TEXT NOT NULL PRIMARY KEY,
                    name                TEXT NOT NULL,
                    type                TEXT NOT NULL,
                    amount              INTEGER,
                    description         TEXT NOT NULL DEFAULT '',
                    categoryId          TEXT REFERENCES categories(categoryId) ON DELETE SET NULL,
                    accountId           TEXT NOT NULL REFERENCES accounts(accountId) ON DELETE RESTRICT,
                    frequency           TEXT NOT NULL DEFAULT 'Monthly',
                    dayOfMonth          INTEGER NOT NULL,
                    isActive            INTEGER NOT NULL DEFAULT 1,
                    lastConfirmedPeriod TEXT,
                    createdAt           INTEGER NOT NULL,
                    updatedAt           INTEGER NOT NULL,
                    userId              TEXT,
                    deletedAt           INTEGER,
                    syncState           TEXT NOT NULL DEFAULT 'Pending'
                );
                """.trimIndent(),
                0,
            )
            listOf(
                "CREATE INDEX IF NOT EXISTS transactions_account_idx ON transactions(accountId)",
                "CREATE INDEX IF NOT EXISTS transactions_date_idx ON transactions(date)",
                "CREATE INDEX IF NOT EXISTS transactions_category_idx ON transactions(categoryId)",
                "CREATE INDEX IF NOT EXISTS transactions_syncstate_idx ON transactions(syncState)",
                "CREATE INDEX IF NOT EXISTS transactions_user_deleted_idx ON transactions(userId, deletedAt)",
            ).forEach { driver.execute(null, it, 0) }
            return QueryResult.Unit
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: app.cash.sqldelight.db.AfterVersion,
        ): QueryResult.Value<Unit> = QueryResult.Unit
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        driver = AndroidSqliteDriver(
            schema = schemaV3,
            context = context,
            name = null,
            callback = object : AndroidSqliteDriver.Callback(schema = schemaV3) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            },
        )
        database = EmmDatabaseData(driver)

        // Raw SQL: the generated TransactionsQueries.insert targets v4 and would reference a
        // column this schema does not have yet.
        exec(
            "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('A1', 'BCP', 'Bank', 'PEN', 1000, 1000)",
        )
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) " +
                "VALUES ('C1', 'Sueldo', 'salary', 'green', 'Income', 0, 1000, 1000)",
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun migration_v3_to_v4_converts_the_instant_to_local_text() {
        // 1_754_000_000_000 ms is 2025-07-31 22:13:20 UTC, which is 17:13:20 in Lima.
        insertV3Transaction(id = "TX1", date = 1_754_000_000_000L)

        EmmDatabaseData.Schema.migrate(driver, oldVersion = 3, newVersion = EmmDatabaseData.Schema.version)

        val tx = database.transactionsQueries.find("TX1").executeAsOneOrNull()
        assertNotNull(tx, "transaction must survive the rebuild")
        assertEquals("2025-07-31T17:13:20", tx.occurredAt)
        assertEquals(350_000L, tx.amount, "the rest of the row must come across untouched")
        assertEquals("Sueldo mayo", tx.description)
        assertEquals("C1", tx.categoryId)
        assertEquals("A1", tx.accountId)
        assertEquals("Pending", tx.syncState)
    }

    @Test
    fun migration_v3_to_v4_keeps_every_row() {
        // The assertion a table rebuild actually needs: DROP TABLE is in this migration, and a
        // copy that silently moved nothing looks exactly like a copy that moved everything until
        // something counts.
        repeat(ROW_COUNT) { index -> insertV3Transaction(id = "TX$index", date = 1_754_000_000_000L + index) }
        val before = rawCount("SELECT COUNT(*) FROM transactions")

        EmmDatabaseData.Schema.migrate(driver, oldVersion = 3, newVersion = EmmDatabaseData.Schema.version)

        assertEquals(ROW_COUNT.toLong(), before)
        assertEquals(before, rawCount("SELECT COUNT(*) FROM transactions"))
    }

    @Test
    fun migration_v3_to_v4_recreates_every_index() {
        // The indexes belong to the table 3.sqm drops. Nothing else in the build would notice they
        // were gone — the app would just get slower, on the owner's device, silently.
        insertV3Transaction(id = "TX1", date = 1_754_000_000_000L)

        EmmDatabaseData.Schema.migrate(driver, oldVersion = 3, newVersion = EmmDatabaseData.Schema.version)

        val indexes = indexNames()
        listOf(
            "transactions_account_idx",
            "transactions_occurred_idx",
            "transactions_category_idx",
            "transactions_syncstate_idx",
            "transactions_user_deleted_idx",
        ).forEach { name ->
            assertTrue(name in indexes, "index $name must exist after the rebuild, found: $indexes")
        }
        assertTrue("transactions_date_idx" !in indexes, "the index on the dropped column must be gone")
    }

    @Test
    fun migration_v3_to_v4_survives_a_date_no_conversion_can_read() {
        // strftime() returns NULL outside its range, and a NULL into a NOT NULL column aborts
        // onUpgrade — leaving user_version at 3 and failing the same way on every later open.
        // The app must open. The row is allowed to be visibly wrong; it is not allowed to be fatal.
        insertV3Transaction(id = "TX-OK", date = 1_754_000_000_000L)
        insertV3Transaction(id = "TX-BROKEN", date = Long.MAX_VALUE)

        EmmDatabaseData.Schema.migrate(driver, oldVersion = 3, newVersion = EmmDatabaseData.Schema.version)

        val broken = database.transactionsQueries.find("TX-BROKEN").executeAsOneOrNull()
        assertNotNull(broken, "the unreadable row must survive rather than abort the migration")
        assertEquals("1969-12-31T19:00:00", broken.occurredAt, "the epoch under the same offset")

        val healthy = database.transactionsQueries.find("TX-OK").executeAsOneOrNull()
        assertNotNull(healthy, "one unreadable row must not cost the readable ones")
        assertEquals("2025-07-31T17:13:20", healthy.occurredAt)
    }

    @Test
    fun migration_v3_to_v4_leaves_the_other_tables_alone() {
        insertV3Transaction(id = "TX1", date = 1_754_000_000_000L)

        EmmDatabaseData.Schema.migrate(driver, oldVersion = 3, newVersion = EmmDatabaseData.Schema.version)

        assertNotNull(database.accountsQueries.find("A1").executeAsOneOrNull())
        assertNotNull(database.categoriesQueries.find("C1").executeAsOneOrNull())
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun exec(sql: String) = driver.execute(null, sql, 0)

    private fun insertV3Transaction(id: String, date: Long) = exec(
        "INSERT INTO transactions(transactionId, type, amount, description, date, categoryId, " +
            "accountId, createdAt, updatedAt) " +
            "VALUES ('$id', 'Income', 350000, 'Sueldo mayo', $date, 'C1', 'A1', 2000, 2000)",
    )

    private fun rawCount(sql: String): Long = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 0,
    ).value

    private fun indexNames(): List<String> = driver.executeQuery(
        identifier = null,
        sql = "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'transactions'",
        mapper = { cursor ->
            val names = mutableListOf<String>()
            while (cursor.next().value) {
                cursor.getString(0)?.let(names::add)
            }
            QueryResult.Value(names.toList())
        },
        parameters = 0,
    ).value

    private companion object {
        const val ROW_COUNT = 25
    }
}
