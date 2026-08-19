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

@RunWith(AndroidJUnit4::class)
class MigrationV4ToV5Test {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: EmmDatabaseData

    private var openedDb: SupportSQLiteDatabase? = null

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
                // Deliberately NOT enabling foreign keys here: seeding the orphan row needs them
                // off. The handle is captured instead so a test can flip them on when it chooses.
                override fun onOpen(db: SupportSQLiteDatabase) {
                    openedDb = db
                }
            },
        )
        database = EmmDatabaseData(driver)

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

    @Test
    fun migration_v4_to_v5_strips_a_category_of_the_wrong_type_off_a_transaction() {
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
        // Without this case, a repair that nulled every categoryId would pass the mismatch test.
        insertV4Transaction(id = "TX-OK", type = "Income", categoryId = "'C-INCOME'")

        migrate()

        val tx = database.transactionsQueries.find("TX-OK").executeAsOneOrNull()
        assertNotNull(tx)
        assertEquals("C-INCOME", tx.categoryId, "a matching category must be left alone")
        assertEquals("Income", tx.type)
    }

    @Test
    fun migration_v4_to_v5_strips_a_category_that_does_not_exist_at_all() {
        insertV4Transaction(id = "TX-ORPHAN", type = "Spend", categoryId = "'C-GONE'")

        migrate()

        val tx = database.transactionsQueries.find("TX-ORPHAN").executeAsOneOrNull()
        assertNotNull(tx)
        assertNull(tx.categoryId, "a dangling category id must be dropped")
        assertEquals("Spend", tx.type)
    }

    @Test
    fun migration_v4_to_v5_leaves_an_uncategorized_transaction_alone() {
        insertV4Transaction(id = "TX-NULL", type = "Spend", categoryId = "NULL")

        migrate()

        val tx = database.transactionsQueries.find("TX-NULL").executeAsOneOrNull()
        assertNotNull(tx, "an uncategorized movement must cross like any other")
        assertNull(tx.categoryId)
        assertEquals("Spend", tx.type)
    }

    @Test
    fun migration_v4_to_v5_never_rewrites_the_type_of_any_transaction() {
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

    @Test
    fun migration_v4_to_v5_repairs_recurring_movements_the_same_way() {
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

    // The only case that can fail on statement order: `INSERT INTO transactions_new SELECT` is
    // validated against the new key as it copies, so 4.sqm has to repair before it rebuilds.
    @Test
    fun the_migration_completes_with_foreign_keys_enabled_the_way_ios_runs_it() {
        insertV4Transaction(id = "TX-MISMATCH", type = "Income", categoryId = "'C-SPEND'")
        insertV4Transaction(id = "TX-OK", type = "Income", categoryId = "'C-INCOME'")
        insertV4Transaction(id = "TX-ORPHAN", type = "Spend", categoryId = "'C-GONE'")
        insertV4Transaction(id = "TX-NULL", type = "Spend", categoryId = "NULL")
        insertV4Recurring(id = "RM-MISMATCH", type = "Spend", categoryId = "'C-INCOME'")
        insertV4Recurring(id = "RM-OK", type = "Spend", categoryId = "'C-SPEND'")

        enableForeignKeys()
        migrate()

        assertNull(database.transactionsQueries.find("TX-MISMATCH").executeAsOne().categoryId)
        assertNull(database.transactionsQueries.find("TX-ORPHAN").executeAsOne().categoryId)
        assertNull(database.transactionsQueries.find("TX-NULL").executeAsOne().categoryId)
        assertEquals("C-INCOME", database.transactionsQueries.find("TX-OK").executeAsOne().categoryId)
        assertNull(database.recurring_movementsQueries.find("RM-MISMATCH").executeAsOne().categoryId)
        assertEquals("C-SPEND", database.recurring_movementsQueries.find("RM-OK").executeAsOne().categoryId)
        assertEquals(4L, rawCount("SELECT COUNT(*) FROM transactions"))
        assertEquals(2L, rawCount("SELECT COUNT(*) FROM recurring_movements"))
    }

    @Test
    fun nothing_violates_the_new_key_once_the_migration_has_run() {
        insertV4Transaction(id = "TX-MISMATCH", type = "Income", categoryId = "'C-SPEND'")
        insertV4Transaction(id = "TX-ORPHAN", type = "Spend", categoryId = "'C-GONE'")
        insertV4Recurring(id = "RM-MISMATCH", type = "Spend", categoryId = "'C-INCOME'")

        migrate()

        assertEquals(emptyList(), foreignKeyViolations(), "no row may violate the schema it just migrated to")
    }

    @Test
    fun migration_v4_to_v5_recreates_every_index() {
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
        enableForeignKeys()

        // SQLiteConstraintException specifically: a missing parent unique index refuses the insert
        // too, as a plain SQLiteException nothing in :data catches.

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

    private fun migrate() =
        EmmDatabaseData.Schema.migrate(driver, oldVersion = 4, newVersion = EmmDatabaseData.Schema.version)

    /**
     * `setForeignKeyConstraintsEnabled` is illegal inside a transaction; both call sites are
     * outside one, and `Schema.migrate` opens none of its own (the real upgrade's transaction
     * belongs to `SQLiteOpenHelper`, which this test bypasses).
     */
    private fun enableForeignKeys() {
        val db = openedDb ?: error("onOpen never fired — the driver was never used")
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun foreignKeyViolations(): List<String> = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA foreign_key_check",
        mapper = { cursor ->
            val rows = mutableListOf<String>()
            while (cursor.next().value) {
                rows += "${cursor.getString(0)}:${cursor.getLong(1)}"
            }
            QueryResult.Value(rows.toList())
        },
        parameters = 0,
    ).value

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
