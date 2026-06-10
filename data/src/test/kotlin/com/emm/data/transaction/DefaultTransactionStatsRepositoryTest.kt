package com.emm.data.transaction

import com.emm.data.TopUsedCombos
import com.emm.domain.transaction.TransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

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

        val result = repository.topUsedCombos(TransactionType.Income, 0L, 10)

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

        val result = repository.topUsedCombos(TransactionType.Income, 0L, 10)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `topUsedCombos - all valid types returns all combos`() = runTest {
        coEvery { localDataSource.topUsedCombos(any(), any(), any()) } returns listOf(
            TopUsedCombos(accountId = "acc-1", categoryId = "cat-1", type = "Income"),
            TopUsedCombos(accountId = "acc-2", categoryId = "cat-2", type = "Spend"),
        )

        val result = repository.topUsedCombos(TransactionType.Income, 0L, 10)

        assertEquals(2, result.size)
        assertEquals(TransactionType.Income, result[0].type)
        assertEquals(TransactionType.Spend, result[1].type)
    }
}
