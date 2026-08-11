package com.emm.data.backup

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
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
            db = db,
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `a version 1 file still restores everything it carries`() = runTest {
        val stats = repository.importFromJson(V1_BACKUP)

        assertEquals(ImportStats(accounts = 1, categories = 2, transactions = 3), stats)
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
