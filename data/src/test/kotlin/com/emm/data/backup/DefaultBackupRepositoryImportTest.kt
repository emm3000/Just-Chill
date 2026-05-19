package com.emm.data.backup

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

// Uses an in-memory JVM SQLite driver (app.cash.sqldelight:sqlite-driver) to exercise
// the full SQLDelight schema — including the FK-ordered delete, the atomicity of the
// transaction block, and the actual insert queries.
// Trade-off: the in-memory driver bootstraps from EmmDatabaseData.Schema.create(), so it
// picks up all table definitions. The Android-specific callback (foreign key PRAGMA, seed
// data) is not run here — FK enforcement is therefore NOT active in this test environment.
// The delete-order logic is still exercised; FK errors would only surface with enforcement on.
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
    fun `importing twice replaces all rows - deleteAll then re-insert`() = runTest {
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

        val emptyJson = """{"schemaVersion":1,"exportedAt":0,"appVersion":"1.0.0","accounts":[],"categories":[],"transactions":[]}"""
        val stats = repository.importFromJson(emptyJson)

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
        val json = """{"schemaVersion":99,"exportedAt":0,"appVersion":"1.0.0","accounts":[],"categories":[],"transactions":[]}"""
        val beforeAccounts = db.accountsQueries.all().executeAsList().size

        assertFailsWith<DomainException.ValidationError> {
            repository.importFromJson(json)
        }

        assertEquals(beforeAccounts, db.accountsQueries.all().executeAsList().size)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

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
            """{"transactionId":"tx-$i","type":"Spend","amountCents":1000,"description":"Tx $i","date":0,"accountId":"acc-1","categoryId":null}"""
        }
        return """
            {
                "schemaVersion": 1,
                "exportedAt": 0,
                "appVersion": "1.0.0",
                "accounts": [$accountsJson],
                "categories": [$categoriesJson],
                "transactions": [$transactionsJson]
            }
        """.trimIndent()
    }
}
