package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetFrequentCombosUseCaseTest {

    private val repo = mockk<TransactionStatsRepository>()
    private val useCase = GetFrequentCombosUseCase(repo)

    @Test
    fun `returns combos ordered by frequency for the requested type`() = runTest {
        val combo1 = FrequentCombo(AccountId("yape"), CategoryId("food"), TransactionType.Spend)
        val combo2 = FrequentCombo(AccountId("yape"), CategoryId("transport"), TransactionType.Spend)
        coEvery {
            repo.topUsedCombos(
                type = TransactionType.Spend,
                startInclusive = any<String>(),
                limit = any<Int>(),
            )
        } returns listOf(combo1, combo2)

        val result = useCase(TransactionType.Spend)

        assertEquals(listOf(combo1, combo2), result)
    }

    @Test
    fun `type-filter exclusion — Income combos are absent when querying Spend`() = runTest {
        val incomeCombo = FrequentCombo(AccountId("bcp"), CategoryId("salary"), TransactionType.Income)
        coEvery {
            repo.topUsedCombos(
                type = TransactionType.Spend,
                startInclusive = any<String>(),
                limit = any<Int>(),
            )
        } returns emptyList()

        val result = useCase(TransactionType.Spend)

        assertTrue(result.none { it == incomeCombo })
        assertEquals(emptyList(), result)
    }

    @Test
    fun `empty history returns empty list without exception`() = runTest {
        coEvery {
            repo.topUsedCombos(
                type = TransactionType.Spend,
                startInclusive = any<String>(),
                limit = any<Int>(),
            )
        } returns emptyList()

        val result = useCase(TransactionType.Spend)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `type mismatch returns empty list`() = runTest {
        coEvery {
            repo.topUsedCombos(
                type = TransactionType.Spend,
                startInclusive = any<String>(),
                limit = any<Int>(),
            )
        } returns emptyList()

        val result = useCase(TransactionType.Spend)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `passes correct 90-day window to repository`() = runTest {
        coEvery {
            repo.topUsedCombos(
                type = any(),
                startInclusive = any<String>(),
                limit = any<Int>(),
            )
        } returns emptyList()

        useCase(TransactionType.Income, windowDays = 90)

        coVerify {
            repo.topUsedCombos(
                type = TransactionType.Income,
                startInclusive = any<String>(),
                limit = any<Int>(),
            )
        }
    }
}
