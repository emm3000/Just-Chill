package com.emm.data.backup

import com.emm.data.EmmDatabaseData
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountType
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultBackupRepositoryTest {

    private val transactionRepo = mockk<TransactionRepository>()
    private val categoryRepo = mockk<CategoryRepository>()
    private val accountRepo = mockk<AccountRepository>()
    private val db = mockk<EmmDatabaseData>(relaxed = true)

    private val repository = DefaultBackupRepository(transactionRepo, categoryRepo, accountRepo, db)

    private val account = Account(
        accountId = AccountId("acc-1"),
        name = "Yape",
        type = AccountType.Cash,
    )

    private val categoryIncome = Category(
        categoryId = CategoryId("cat-1"),
        name = "Sueldo",
        icon = "work",
        color = "#00FF00",
        categoryType = CategoryType.Income,
    )

    private val categorySpend = Category(
        categoryId = CategoryId("cat-2"),
        name = "Comida",
        icon = "food",
        color = "#FF0000",
        categoryType = CategoryType.Spend,
    )

    private val transactionWithCategory = Transaction(
        transactionId = TransactionId("tx-1"),
        type = TransactionType.Income,
        amount = Money(4500_00L),
        description = "Sueldo mayo",
        date = 1_748_000_000_000L,
        accountId = AccountId("acc-1"),
        categoryId = CategoryId("cat-1"),
    )

    private val transactionNullCategory = Transaction(
        transactionId = TransactionId("tx-2"),
        type = TransactionType.Spend,
        amount = Money(150_00L),
        description = "Almuerzo",
        date = 1_748_100_000_000L,
        accountId = AccountId("acc-1"),
        categoryId = null,
    )

    @Test
    fun `produces JSON containing all accounts, categories and transactions`() = runTest {
        every { accountRepo.all() } returns flowOf(listOf(account))
        every { categoryRepo.all() } returns flowOf(listOf(categoryIncome, categorySpend))
        every { transactionRepo.all() } returns flowOf(listOf(transactionWithCategory, transactionNullCategory))

        val json = repository.exportToJson(exportedAt = 1_748_000_000_000L, appVersion = "1.0.0")

        val payload = Json.decodeFromString<ExportPayloadDto>(json)
        assertEquals(1, payload.schemaVersion)
        assertEquals(1_748_000_000_000L, payload.exportedAt)
        assertEquals("1.0.0", payload.appVersion)
        assertEquals(1, payload.accounts.size)
        assertEquals("acc-1", payload.accounts[0].accountId)
        assertEquals("Yape", payload.accounts[0].name)
        assertEquals(2, payload.categories.size)
        assertEquals(2, payload.transactions.size)
        assertEquals(4500_00L, payload.transactions[0].amountCents)
        assertEquals(150_00L, payload.transactions[1].amountCents)
    }

    @Test
    fun `a categoryId whose category is gone is exported as null`() = runTest {
        // Deleting a category no longer nulls the column on its movements, so a live transaction
        // can point at a tombstoned category. Tombstoned categories are not exported, so leaving
        // the id in the file would produce a backup that fails its own import on the FK.
        val orphan = transactionWithCategory.copy(
            transactionId = TransactionId("tx-3"),
            categoryId = CategoryId("cat-deleted"),
        )
        every { accountRepo.all() } returns flowOf(listOf(account))
        every { categoryRepo.all() } returns flowOf(listOf(categoryIncome))
        every { transactionRepo.all() } returns flowOf(listOf(transactionWithCategory, orphan))

        val json = repository.exportToJson(exportedAt = 0L, appVersion = "1.0.0")

        val payload = Json.decodeFromString<ExportPayloadDto>(json)
        val exportedIds = payload.categories.map { it.categoryId }
        assertEquals(listOf("cat-1"), exportedIds)
        assertEquals("cat-1", payload.transactions.single { it.transactionId == "tx-1" }.categoryId)
        assertNull(payload.transactions.single { it.transactionId == "tx-3" }.categoryId)
    }

    @Test
    fun `empty state - produces valid JSON with schemaVersion 1 and empty arrays`() = runTest {
        every { accountRepo.all() } returns flowOf(emptyList())
        every { categoryRepo.all() } returns flowOf(emptyList())
        every { transactionRepo.all() } returns flowOf(emptyList())

        val json = repository.exportToJson(exportedAt = 0L, appVersion = "1.0.0")

        val payload = Json.decodeFromString<ExportPayloadDto>(json)
        assertEquals(1, payload.schemaVersion)
        assertTrue(payload.accounts.isEmpty())
        assertTrue(payload.categories.isEmpty())
        assertTrue(payload.transactions.isEmpty())
    }

    @Test
    fun `JSON output is pretty-printed`() = runTest {
        every { accountRepo.all() } returns flowOf(emptyList())
        every { categoryRepo.all() } returns flowOf(emptyList())
        every { transactionRepo.all() } returns flowOf(emptyList())

        val json = repository.exportToJson(exportedAt = 0L, appVersion = "1.0.0")

        assertTrue(json.contains('\n'))
    }
}
