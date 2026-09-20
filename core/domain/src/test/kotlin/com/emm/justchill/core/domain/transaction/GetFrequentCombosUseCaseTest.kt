package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
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

    private val noonClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-11T12:00:00Z")
    }

    private val useCase = GetFrequentCombosUseCase(repo, noonClock, TimeZone.UTC)

    private fun useCaseAt(clock: Clock): GetFrequentCombosUseCase = GetFrequentCombosUseCase(repo, clock, TimeZone.UTC)

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
                startInclusive = "2026-05-13",
                limit = any<Int>(),
            )
        }
    }

    @Test
    fun `a combo inside the typed amount's band and the current hour's window outranks a globally more frequent one`() =
        runTest {
            val frequentButOffContext = FrequentCombo(AccountId("yape"), CategoryId("transport"), TransactionType.Spend)
            val rareButInContext = FrequentCombo(AccountId("bcp"), CategoryId("food"), TransactionType.Spend)
            coEvery {
                repo.comboOccurrences(type = TransactionType.Spend, startInclusive = any<String>())
            } returns listOf(
                occurrence(frequentButOffContext, cents = 500L, hour = "20"),
                occurrence(frequentButOffContext, cents = 500L, hour = "21"),
                occurrence(frequentButOffContext, cents = 500L, hour = "22"),
                occurrence(rareButInContext, cents = 5000L, hour = "12"),
            )

            val result = useCase(TransactionType.Spend, amount = Money(5000L))

            assertEquals(rareButInContext, result.first())
        }

    @Test
    fun `with no amount typed the order is exactly today's`() = runTest {
        val combo1 = FrequentCombo(AccountId("yape"), CategoryId("food"), TransactionType.Spend)
        val combo2 = FrequentCombo(AccountId("yape"), CategoryId("transport"), TransactionType.Spend)
        coEvery {
            repo.topUsedCombos(type = TransactionType.Spend, startInclusive = any<String>(), limit = any<Int>())
        } returns listOf(combo1, combo2)

        val result = useCase(TransactionType.Spend)

        assertEquals(listOf(combo1, combo2), result)
        coVerify(exactly = 0) { repo.comboOccurrences(any(), any()) }
    }

    @Test
    fun `two fake clocks over the same rows return a different first combo`() = runTest {
        val morningCombo = FrequentCombo(AccountId("bcp"), CategoryId("food"), TransactionType.Spend)
        val nightCombo = FrequentCombo(AccountId("yape"), CategoryId("entertainment"), TransactionType.Spend)
        coEvery {
            repo.comboOccurrences(type = TransactionType.Spend, startInclusive = any<String>())
        } returns listOf(
            occurrence(morningCombo, cents = 2000L, hour = "08"),
            occurrence(nightCombo, cents = 2000L, hour = "22"),
        )

        val morningResult = useCaseAt(fixedHourClock("08")).invoke(TransactionType.Spend, amount = Money(2000L))
        val nightResult = useCaseAt(fixedHourClock("22")).invoke(TransactionType.Spend, amount = Money(2000L))

        assertEquals(morningCombo, morningResult.first())
        assertEquals(nightCombo, nightResult.first())
    }

    @Test
    fun `an empty history returns an empty list without throwing`() = runTest {
        coEvery {
            repo.comboOccurrences(type = TransactionType.Spend, startInclusive = any<String>())
        } returns emptyList()

        val result = useCase(TransactionType.Spend, amount = Money(2000L))

        assertTrue(result.isEmpty())
    }

    private fun occurrence(combo: FrequentCombo, cents: Long, hour: String): ComboOccurrence = ComboOccurrence(
        accountId = combo.accountId,
        categoryId = combo.categoryId,
        type = combo.type,
        amount = Money(cents),
        occurredAt = "2026-08-11T$hour:00:00",
    )

    private fun fixedHourClock(hour: String): Clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-11T$hour:00:00Z")
    }
}
