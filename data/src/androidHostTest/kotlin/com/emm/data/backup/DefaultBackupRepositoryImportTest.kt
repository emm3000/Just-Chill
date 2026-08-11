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
import kotlin.test.assertTrue

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

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        exec("PRAGMA foreign_keys=ON")
        repository = DefaultBackupRepository(
            transactions = transactionRepo,
            categories = categoryRepo,
            accounts = accountRepo,
            db = db,
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

    private fun rawCount(sql: String): Long = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 0,
    ).value

    /**
     * Builds a minimal valid JSON payload. Account IDs are sequential to ensure uniqueness.
     * Transaction accountId always references "acc-1" which is in the accounts list.
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

    private companion object {
        const val EMPTY_PAYLOAD_JSON = """{"schemaVersion":2,"exportedAt":0,"appVersion":"1.0.0",""" +
            """"accounts":[],"categories":[],"transactions":[]}"""
    }
}
