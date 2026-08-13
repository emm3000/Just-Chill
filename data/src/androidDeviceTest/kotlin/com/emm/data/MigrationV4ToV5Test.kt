package com.emm.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Migration test: schema version 4 → 5 — a movement's category must share the movement's type, and
 * the schema is what says so from here on (composite FK `(categoryId, type)` → `categories`).
 *
 * The second DESTRUCTIVE migration in this repository, and the first that rebuilds TWO tables. Both
 * `transactions` and `recurring_movements` are dropped and recreated, and 4.sqm repairs the data
 * BEFORE it rebuilds — on iOS the copy is validated against the new key as it runs, so a repair
 * placed after the rebuild would be repairing rows that never crossed.
 *
 * What is asserted, and why each one:
 *   1. a movement whose category has the OTHER type loses the category — the defect being repaired;
 *   2. a movement whose category matches keeps it — a repair that nulls everything also "passes" 1;
 *   3. a movement pointing at a category that does not exist loses it too — the orphan shape the
 *      new key rejects just as hard, and the reason the repair is `NOT EXISTS` and not a comparison;
 *   4. a movement with no category is untouched — a composite FK with a NULL column is SATISFIED,
 *      so uncategorized movements must need no special case;
 *   5. **`type` is unchanged on every row.** The sign of `amount` follows the type; a repair that
 *      "fixed" the mismatch from the other side would rewrite the user's balance, silently and
 *      irreversibly. This is the assertion that says it did not;
 *   6. every row survives on BOTH tables — two DROP TABLEs, and a copy that moved nothing looks
 *      exactly like a copy that moved everything until something counts;
 *   7. every index exists afterwards — they belong to the dropped tables and nothing else in the
 *      build would ever notice their absence;
 *   8. the composite key is really enforced afterwards, as a CONSTRAINT violation. If the parent
 *      UNIQUE index were missing, SQLite would still refuse the insert — with a generic "foreign
 *      key mismatch", which is a different exception type and one no caller in `:data` catches.
 *
 * Migrates to `EmmDatabaseData.Schema.version`, never to a hardcoded 5 — the rule in
 * `data/CLAUDE.md`, written after `MigrationV2ToV3Test` stopped at 3 and broke the moment a 4
 * existed.
 *
 * Foreign keys are left OFF while the fixture is seeded, which is exactly what a real Android
 * upgrade does (`onOpen` runs after `onUpgrade`) and the only way to plant the orphan row case 3
 * needs. The one test that asserts enforcement turns them on explicitly, after the migration.
 *
 * Requires a device/emulator. Run with: `./gradlew :data:connectedAndroidDeviceTest`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationV4ToV5Test {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: EmmDatabaseData

    /** Captured in [AndroidSqliteDriver.Callback.onOpen] — the only handle that can toggle FKs. */
    private var openedDb: SupportSQLiteDatabase? = null

    /**
     * The production schema at version 4 — all 4 tables and every index exactly as they existed
     * before this change: single-column `categoryId` foreign keys, and no unique index on
     * `categories(categoryId, categoryType)`.
     */
    private val schemaV4 = object : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 4

        // Four CREATE TABLEs and ten indexes, verbatim. Splitting them up would hide the one thing
        // this block is for: being readable as the schema it claims to reproduce.
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
                    occurredAt     TEXT NOT NULL,
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
                "CREATE INDEX IF NOT EXISTS accounts_syncstate_idx ON accounts(syncState)",
                "CREATE INDEX IF NOT EXISTS accounts_user_deleted_idx ON accounts(userId, deletedAt)",
                "CREATE INDEX IF NOT EXISTS categories_type_idx ON categories(categoryType)",
                "CREATE INDEX IF NOT EXISTS categories_syncstate_idx ON categories(syncState)",
                "CREATE INDEX IF NOT EXISTS categories_user_deleted_idx ON categories(userId, deletedAt)",
                "CREATE INDEX IF NOT EXISTS transactions_account_idx ON transactions(accountId)",
                "CREATE INDEX IF NOT EXISTS transactions_occurred_idx ON transactions(occurredAt)",
                "CREATE INDEX IF NOT EXISTS transactions_category_idx ON transactions(categoryId)",
                "CREATE INDEX IF NOT EXISTS transactions_syncstate_idx ON transactions(syncState)",
                "CREATE INDEX IF NOT EXISTS transactions_user_deleted_idx ON transactions(userId, deletedAt)",
                "CREATE INDEX IF NOT EXISTS recurring_active_idx ON recurring_movements(isActive, name)",
                "CREATE INDEX IF NOT EXISTS recurring_movements_syncstate_idx ON recurring_movements(syncState)",
                "CREATE INDEX IF NOT EXISTS recurring_movements_user_deleted_idx " +
                    "ON recurring_movements(userId, deletedAt)",
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
            schema = schemaV4,
            context = context,
            name = null,
            callback = object : AndroidSqliteDriver.Callback(schema = schemaV4) {
                // Deliberately NOT enabling foreign keys: that is what a real Android upgrade sees
                // (onOpen runs after onUpgrade), and case 3 needs to plant a row the old
                // single-column FK would have rejected.
                override fun onOpen(db: SupportSQLiteDatabase) {
                    openedDb = db
                }
            },
        )
        database = EmmDatabaseData(driver)

        // Raw SQL against the historical schema: the generated queries target v5 and carry the
        // constraint this fixture exists to violate.
        exec(
            "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('A1', 'BCP', 'Bank', 'PEN', 1000, 1000)",
        )
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) " +
                "VALUES ('C-INCOME', 'Sueldo', 'salary', 'green', 'Income', 0, 1000, 1000)",
        )
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) " +
                "VALUES ('C-SPEND', 'Café', 'coffee', 'brown', 'Spend', 0, 1000, 1000)",
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    // ── transactions ──────────────────────────────────────────────────────────

    @Test
    fun migration_v4_to_v5_strips_a_category_of_the_wrong_type_off_a_transaction() {
        // The defect this migration exists for: an Income movement filed under a Spend category.
        insertV4Transaction(id = "TX-MISMATCH", type = "Income", categoryId = "'C-SPEND'")

        migrate()

        val tx = database.transactionsQueries.find("TX-MISMATCH").executeAsOneOrNull()
        assertNotNull(tx, "the row must survive the rebuild, not be deleted with its category")
        assertNull(tx.categoryId, "a category of the other type must be dropped")
        assertEquals("Income", tx.type, "the type is the truth and must never be rewritten")
        assertEquals(350_000L, tx.amount, "the rest of the row comes across untouched")
        assertEquals("Sueldo mayo", tx.description)
        assertEquals("A1", tx.accountId)
    }

    @Test
    fun migration_v4_to_v5_keeps_a_category_that_already_matches() {
        // Without this, a repair that simply nulled every categoryId would pass the test above.
        insertV4Transaction(id = "TX-OK", type = "Income", categoryId = "'C-INCOME'")

        migrate()

        val tx = database.transactionsQueries.find("TX-OK").executeAsOneOrNull()
        assertNotNull(tx)
        assertEquals("C-INCOME", tx.categoryId, "a matching category must be left alone")
        assertEquals("Income", tx.type)
    }

    @Test
    fun migration_v4_to_v5_strips_a_category_that_does_not_exist_at_all() {
        // Orphans violate the new key just as hard as mismatches, which is why the repair asks
        // NOT EXISTS rather than comparing the two type columns.
        insertV4Transaction(id = "TX-ORPHAN", type = "Spend", categoryId = "'C-GONE'")

        migrate()

        val tx = database.transactionsQueries.find("TX-ORPHAN").executeAsOneOrNull()
        assertNotNull(tx)
        assertNull(tx.categoryId, "a dangling category id must be dropped")
        assertEquals("Spend", tx.type)
    }

    @Test
    fun migration_v4_to_v5_leaves_an_uncategorized_transaction_alone() {
        // A composite FK with any NULL column is SATISFIED, so this needs no special case anywhere.
        insertV4Transaction(id = "TX-NULL", type = "Spend", categoryId = "NULL")

        migrate()

        val tx = database.transactionsQueries.find("TX-NULL").executeAsOneOrNull()
        assertNotNull(tx, "an uncategorized movement must cross like any other")
        assertNull(tx.categoryId)
        assertEquals("Spend", tx.type)
    }

    @Test
    fun migration_v4_to_v5_never_rewrites_the_type_of_any_transaction() {
        // The whole point of repairing from the category side. `amount` is signed BY the type in
        // getAccountBalance and liveTotals, so flipping one row's type moves the user's balance.
        val seeded = listOf(
            Triple("TX-A", "Income", "'C-SPEND'"),
            Triple("TX-B", "Spend", "'C-INCOME'"),
            Triple("TX-C", "Income", "'C-INCOME'"),
            Triple("TX-D", "Spend", "NULL"),
            Triple("TX-E", "Income", "'C-GONE'"),
        )
        seeded.forEach { (id, type, category) -> insertV4Transaction(id, type, category) }

        migrate()

        seeded.forEach { (id, type, _) ->
            val tx = database.transactionsQueries.find(id).executeAsOneOrNull()
            assertNotNull(tx, "$id must survive")
            assertEquals(type, tx.type, "$id changed type — the repair rewrote financial history")
        }
    }

    @Test
    fun migration_v4_to_v5_keeps_every_transaction_row() {
        repeat(ROW_COUNT) { index ->
            val category = if (index % 2 == 0) "'C-INCOME'" else "'C-SPEND'"
            insertV4Transaction(id = "TX$index", type = "Income", categoryId = category)
        }
        val before = rawCount("SELECT COUNT(*) FROM transactions")

        migrate()

        assertEquals(ROW_COUNT.toLong(), before)
        assertEquals(before, rawCount("SELECT COUNT(*) FROM transactions"))
    }

    // ── recurring_movements ───────────────────────────────────────────────────

    @Test
    fun migration_v4_to_v5_repairs_recurring_movements_the_same_way() {
        // A template mints a transaction of its own type every month, so a mismatched template is
        // a mismatch generator, not a single bad row.
        insertV4Recurring(id = "RM-MISMATCH", type = "Spend", categoryId = "'C-INCOME'")
        insertV4Recurring(id = "RM-OK", type = "Spend", categoryId = "'C-SPEND'")
        insertV4Recurring(id = "RM-ORPHAN", type = "Income", categoryId = "'C-GONE'")
        insertV4Recurring(id = "RM-NULL", type = "Income", categoryId = "NULL")

        migrate()

        val mismatch = database.recurring_movementsQueries.find("RM-MISMATCH").executeAsOneOrNull()
        assertNotNull(mismatch)
        assertNull(mismatch.categoryId, "a category of the other type must be dropped")
        assertEquals("Spend", mismatch.type, "the type is the truth and must never be rewritten")

        val ok = database.recurring_movementsQueries.find("RM-OK").executeAsOneOrNull()
        assertNotNull(ok)
        assertEquals("C-SPEND", ok.categoryId, "a matching category must be left alone")

        val orphan = database.recurring_movementsQueries.find("RM-ORPHAN").executeAsOneOrNull()
        assertNotNull(orphan)
        assertNull(orphan.categoryId, "a dangling category id must be dropped")
        assertEquals("Income", orphan.type)

        val uncategorized = database.recurring_movementsQueries.find("RM-NULL").executeAsOneOrNull()
        assertNotNull(uncategorized)
        assertNull(uncategorized.categoryId)
        assertEquals("Income", uncategorized.type)
    }

    @Test
    fun migration_v4_to_v5_keeps_every_recurring_movement_row() {
        repeat(ROW_COUNT) { index ->
            val category = if (index % 2 == 0) "'C-INCOME'" else "'C-SPEND'"
            insertV4Recurring(id = "RM$index", type = "Income", categoryId = category)
        }
        val before = rawCount("SELECT COUNT(*) FROM recurring_movements")

        migrate()

        assertEquals(ROW_COUNT.toLong(), before)
        assertEquals(before, rawCount("SELECT COUNT(*) FROM recurring_movements"))
    }

    // ── structure ─────────────────────────────────────────────────────────────

    @Test
    fun migration_v4_to_v5_recreates_every_index() {
        // The indexes belong to the two tables 4.sqm drops. Nothing else in the build would notice
        // they were gone — the app would just get slower, on the owner's device, silently.
        insertV4Transaction(id = "TX1", type = "Income", categoryId = "'C-INCOME'")
        insertV4Recurring(id = "RM1", type = "Income", categoryId = "'C-INCOME'")

        migrate()

        val transactionIndexes = indexNames("transactions")
        listOf(
            "transactions_account_idx",
            "transactions_occurred_idx",
            "transactions_category_idx",
            "transactions_syncstate_idx",
            "transactions_user_deleted_idx",
        ).forEach { name ->
            assertTrue(name in transactionIndexes, "index $name must exist, found: $transactionIndexes")
        }

        val recurringIndexes = indexNames("recurring_movements")
        listOf(
            "recurring_active_idx",
            "recurring_movements_syncstate_idx",
            "recurring_movements_user_deleted_idx",
        ).forEach { name ->
            assertTrue(name in recurringIndexes, "index $name must exist, found: $recurringIndexes")
        }

        // The parent key of the composite FK. Absent, the child tables still build and every
        // insert then fails at runtime instead.
        assertTrue(
            "categories_id_type_uidx" in indexNames("categories"),
            "the composite FK's parent unique index must exist",
        )
    }

    @Test
    fun migration_v4_to_v5_leaves_the_other_tables_alone() {
        insertV4Transaction(id = "TX1", type = "Income", categoryId = "'C-INCOME'")

        migrate()

        assertNotNull(database.accountsQueries.find("A1").executeAsOneOrNull())
        assertNotNull(database.categoriesQueries.find("C-INCOME").executeAsOneOrNull())
        assertNotNull(database.categoriesQueries.find("C-SPEND").executeAsOneOrNull())
    }

    @Test
    fun the_composite_key_is_really_enforced_after_the_migration() {
        migrate()
        // Enforcement is what the whole change buys; asserting it needs foreign keys ON, which the
        // fixture deliberately left off. SQLiteConstraintException specifically: a missing parent
        // unique index refuses the insert too, as a plain SQLiteException nothing in :data catches.
        enableForeignKeys()

        assertFailsWith<SQLiteConstraintException> {
            database.transactionsQueries.insert(
                transactionId = "TX-NEW-MISMATCH",
                type = "Income",
                amount = 100L,
                description = "",
                occurredAt = "2026-08-12T10:00:00",
                categoryId = "C-SPEND",
                accountId = "A1",
                createdAt = 1_000L,
                updatedAt = 1_000L,
            )
        }
        assertEquals(0L, rawCount("SELECT COUNT(*) FROM transactions WHERE transactionId = 'TX-NEW-MISMATCH'"))

        assertFailsWith<SQLiteConstraintException> {
            database.recurring_movementsQueries.insert(
                id = "RM-NEW-MISMATCH",
                name = "Netflix",
                type = "Spend",
                amount = 100L,
                description = "",
                categoryId = "C-INCOME",
                accountId = "A1",
                frequency = "Monthly",
                dayOfMonth = 1L,
                isActive = 1L,
                lastConfirmedPeriod = null,
                createdAt = 1_000L,
                updatedAt = 1_000L,
            )
        }

        // The matching pair still writes — a key that rejects everything would also pass the above.
        database.transactionsQueries.insert(
            transactionId = "TX-NEW-OK",
            type = "Income",
            amount = 100L,
            description = "",
            occurredAt = "2026-08-12T10:00:00",
            categoryId = "C-INCOME",
            accountId = "A1",
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
        assertNotNull(database.transactionsQueries.find("TX-NEW-OK").executeAsOneOrNull())

        // And so does an uncategorized one: a composite key with a NULL column is satisfied.
        database.transactionsQueries.insert(
            transactionId = "TX-NEW-NULL",
            type = "Income",
            amount = 100L,
            description = "",
            occurredAt = "2026-08-12T10:00:00",
            categoryId = null,
            accountId = "A1",
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
        assertNotNull(database.transactionsQueries.find("TX-NEW-NULL").executeAsOneOrNull())
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun migrate() =
        EmmDatabaseData.Schema.migrate(driver, oldVersion = 4, newVersion = EmmDatabaseData.Schema.version)

    private fun enableForeignKeys() {
        val db = openedDb ?: error("onOpen never fired — the driver was never used")
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun exec(sql: String) = driver.execute(null, sql, 0)

    /** [categoryId] is raw SQL — either a quoted literal or `NULL`, so the null case is expressible. */
    private fun insertV4Transaction(id: String, type: String, categoryId: String) = exec(
        "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
            "accountId, createdAt, updatedAt) " +
            "VALUES ('$id', '$type', 350000, 'Sueldo mayo', '2025-07-31T17:13:20', $categoryId, 'A1', 2000, 2000)",
    )

    /** [categoryId] is raw SQL, same reason as [insertV4Transaction]. */
    private fun insertV4Recurring(id: String, type: String, categoryId: String) = exec(
        "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, accountId, " +
            "frequency, dayOfMonth, isActive, createdAt, updatedAt) " +
            "VALUES ('$id', 'Netflix', '$type', 4490, '', $categoryId, 'A1', 'Monthly', 5, 1, 2000, 2000)",
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

    private fun indexNames(table: String): List<String> = driver.executeQuery(
        identifier = null,
        sql = "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = '$table'",
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
