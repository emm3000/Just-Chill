package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class GetFrequentCombosUseCaseTest {

    private val repo = mockk<TransactionStatsRepository>()

    // Stated, not inherited: the window's lower bound is counted back from this instant, so the
    // suite names it rather than letting the machine answer.
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-11T12:00:00Z")
    }

    private val useCase = GetFrequentCombosUseCase(repo, clock, TimeZone.UTC)

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

        // The date, not `any()`. This test was named for the window it passed and asserted nothing
        // about it — it could not, while the bound was counted back from whatever day the suite ran
        // on. 90 calendar days before 2026-08-11 is 2026-05-13, and `DateWindowTest` derives the
        // same value independently.
        coVerify {
            repo.topUsedCombos(
                type = TransactionType.Income,
                startInclusive = "2026-05-13",
                limit = any<Int>(),
            )
        }
    }
}
