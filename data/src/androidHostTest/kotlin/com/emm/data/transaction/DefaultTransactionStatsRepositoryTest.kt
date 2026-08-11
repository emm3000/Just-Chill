package com.emm.data.transaction

import com.emm.data.TopUsedCombos
import com.emm.domain.report.MonthCategoryAmounts
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.MonthRange
import com.emm.domain.transaction.TransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultTransactionStatsRepositoryTest {

    private val localDataSource: TransactionStatsLocalDataSource = mockk()
    private val repository = DefaultTransactionStatsRepository(localDataSource)

    // ── topUsedCombos ─────────────────────────────────────────────────────────

    @Test
    fun `topUsedCombos - skips combo with unknown type and returns only valid combos`() = runTest {
        coEvery { localDataSource.topUsedCombos(any(), any(), any()) } returns listOf(
            TopUsedCombos(accountId = "acc-1", categoryId = "cat-1", type = "Income"),
            TopUsedCombos(accountId = "acc-2", categoryId = "cat-2", type = "INCOME"), // unknown → skip
            TopUsedCombos(accountId = "acc-3", categoryId = "cat-3", type = "Spend"),
        )

        val result = repository.topUsedCombos(TransactionType.Income, "2026-01-01", 10)

        assertEquals(2, result.size)
        assertEquals("acc-1", result[0].accountId.value)
        assertEquals("acc-3", result[1].accountId.value)
    }

    @Test
    fun `topUsedCombos - all unknown types returns empty list`() = runTest {
        coEvery { localDataSource.topUsedCombos(any(), any(), any()) } returns listOf(
            TopUsedCombos(accountId = "acc-1", categoryId = "cat-1", type = "INCOME"),
            TopUsedCombos(accountId = "acc-2", categoryId = "cat-2", type = "Transfer"),
        )

        val result = repository.topUsedCombos(TransactionType.Income, "2026-01-01", 10)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `topUsedCombos - all valid types returns all combos`() = runTest {
        coEvery { localDataSource.topUsedCombos(any(), any(), any()) } returns listOf(
            TopUsedCombos(accountId = "acc-1", categoryId = "cat-1", type = "Income"),
            TopUsedCombos(accountId = "acc-2", categoryId = "cat-2", type = "Spend"),
        )

        val result = repository.topUsedCombos(TransactionType.Income, "2026-01-01", 10)

        assertEquals(2, result.size)
        assertEquals(TransactionType.Income, result[0].type)
        assertEquals(TransactionType.Spend, result[1].type)
    }

    // ── monthlyAmountByCategoryForRanges ──────────────────────────────────────

    @Test
    fun `monthlyAmountByCategoryForRanges - splits one month's rows into income and expense`() = runTest {
        coEvery { localDataSource.monthlyAmountByCategoryForRanges(any()) } returns listOf(
            listOf(
                row(type = "Spend", categoryId = "cat-1", amount = 1_500),
                row(type = "Income", categoryId = "cat-2", amount = 4_000),
                row(type = "Spend", categoryId = null, amount = 500),
            ),
        )

        val result = repository.monthlyAmountByCategoryForRanges(listOf(MonthRange("2026-01-01", "2026-02-01")))

        val month = result.single()
        assertEquals(listOf(Money(4_000L)), month.income.map { it.amount })
        assertEquals(listOf(Money(1_500L), Money(500L)), month.expense.map { it.amount })
        assertEquals(CategoryId("cat-1"), month.expense.first().categoryId)
        assertNull(month.expense.last().categoryId)
    }

    @Test
    fun `monthlyAmountByCategoryForRanges - keeps one entry per range, in order`() = runTest {
        coEvery { localDataSource.monthlyAmountByCategoryForRanges(any()) } returns listOf(
            listOf(row(type = "Spend", categoryId = "cat-1", amount = 100)),
            emptyList(),
            listOf(row(type = "Income", categoryId = "cat-1", amount = 300)),
        )

        val result = repository.monthlyAmountByCategoryForRanges(
            listOf(
                MonthRange("2026-01-01", "2026-02-01"),
                MonthRange("2026-02-01", "2026-03-01"),
                MonthRange("2026-03-01", "2026-04-01"),
            ),
        )

        // The caller pairs these back up with its own month list by index; dropping an empty month
        // would shift every later month's figures onto the wrong bar.
        assertEquals(3, result.size)
        assertEquals(Money(100L), result[0].expense.single().amount)
        assertEquals(MonthCategoryAmounts.Empty, result[1])
        assertEquals(Money(300L), result[2].income.single().amount)
    }

    @Test
    fun `monthlyAmountByCategoryForRanges - drops rows whose type the app cannot read`() = runTest {
        coEvery { localDataSource.monthlyAmountByCategoryForRanges(any()) } returns listOf(
            listOf(
                row(type = "Spend", categoryId = "cat-1", amount = 1_000),
                row(type = "Transfer", categoryId = "cat-2", amount = 9_000),
                row(type = "INCOME", categoryId = "cat-3", amount = 8_000),
            ),
        )

        val result = repository.monthlyAmountByCategoryForRanges(listOf(MonthRange("2026-01-01", "2026-02-01")))

        assertEquals(Money(1_000L), result.single().expense.single().amount)
        assertEquals(emptyList(), result.single().income)
    }

    private fun row(type: String, categoryId: String?, amount: Long) = MonthlyAmountByTypeEntity(
        type = type,
        categoryId = categoryId,
        categoryName = categoryId?.let { "Cat $it" },
        categoryIcon = categoryId?.let { "icon" },
        categoryColor = categoryId?.let { "blue" },
        totalAmount = amount,
    )
}
