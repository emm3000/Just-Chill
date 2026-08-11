package com.emm.domain.report

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.MonthRange
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class GetTopCategoriesOverMonthsUseCaseTest {

    private val repository = mockk<TransactionStatsRepository>()
    private val useCase = GetTopCategoriesOverMonthsUseCase(repository)

    // Mid-month noon UTC: the window ends on May 2026 in every zone, so the tests below are about
    // ranking, not about boundaries — the boundary has its own test.
    private val fixedClock: Clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-05-15T12:00:00Z")
    }

    private val spendByMonth = mutableMapOf<YearMonth, List<CategoryAmount>>()
    private val incomeByMonth = mutableMapOf<YearMonth, List<CategoryAmount>>()

    @Before
    fun setUp() {
        coEvery { repository.monthlyAmountByCategoryForRanges(any()) } answers {
            firstArg<List<MonthRange>>().map { range ->
                val ym = YearMonth.of(LocalDate.parse(range.startInclusive))
                MonthCategoryAmounts(
                    income = incomeByMonth[ym].orEmpty(),
                    expense = spendByMonth[ym].orEmpty(),
                )
            }
        }
    }

    private fun cat(id: String, amountCents: Long) = CategoryAmount(
        categoryId = CategoryId(id),
        categoryName = "Cat $id",
        categoryIcon = "icon_$id",
        categoryColor = "blue",
        amount = Money(amountCents),
    )

    private fun uncategorized(amountCents: Long) = CategoryAmount(
        categoryId = null,
        categoryName = null,
        categoryIcon = null,
        categoryColor = null,
        amount = Money(amountCents),
    )

    private fun stubMonth(ym: YearMonth, items: List<CategoryAmount>) {
        spendByMonth[ym] = items
    }

    private fun stubIncomeMonth(ym: YearMonth, items: List<CategoryAmount>) {
        incomeByMonth[ym] = items
    }

    @Test
    fun `the window ends at the month of the injected zone, not the device's`() = runTest {
        // One instant, two zones, two different months: 2026-09-01T02:00Z is already September at
        // UTC and still 31 August at UTC-5. A one-month window therefore reads August in Lima and
        // September at UTC — off the same clock. The zone used to come off the machine, where no
        // test can move it onto a boundary.
        //
        // Both zones are asserted because one proves nothing: on a machine whose own clock sits in
        // that zone the ambient read agrees, and the test stays green straight through the bug.
        // The dev machine here is America/Lima, which is exactly UTC-5.
        val nearMidnight: Clock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-09-01T02:00:00Z")
        }
        stubMonth(YearMonth(2026, Month.AUGUST), listOf(cat("A", 100_00L)))

        val atLima = useCase(
            TransactionType.Spend,
            clock = nearMidnight,
            zone = UtcOffset(hours = -5).asTimeZone(),
            months = 1,
            topN = 3,
        )
        val atUtc = useCase(
            TransactionType.Spend,
            clock = nearMidnight,
            zone = UtcOffset(hours = 0).asTimeZone(),
            months = 1,
            topN = 3,
        )

        assertEquals(listOf(CategoryId("A")), atLima.map { it.categoryId })
        assertTrue(atUtc.isEmpty(), "September holds no movements; got: $atUtc")
    }

    @Test
    fun `happy path - returns top 3 categories sorted by total amount`() = runTest {
        val may = YearMonth(2026, Month.MAY)
        stubMonth(may, listOf(cat("A", 500_00L), cat("B", 300_00L), cat("C", 200_00L)))

        val result = useCase(TransactionType.Spend, clock = fixedClock, zone = TimeZone.UTC, months = 1, topN = 3)

        assertEquals(3, result.size)
        assertEquals(CategoryId("A"), result[0].categoryId)
        assertEquals(CategoryId("B"), result[1].categoryId)
        assertEquals(CategoryId("C"), result[2].categoryId)
    }

    @Test
    fun `the whole window is read in a single round-trip, oldest month first`() = runTest {
        val ranges = slot<List<MonthRange>>()

        useCase(TransactionType.Spend, clock = fixedClock, zone = TimeZone.UTC, months = 6, topN = 3)

        coVerify(exactly = 1) { repository.monthlyAmountByCategoryForRanges(capture(ranges)) }
        assertEquals(6, ranges.captured.size)
        val months = ranges.captured.map { YearMonth.of(LocalDate.parse(it.startInclusive)) }
        assertEquals(YearMonth(2025, Month.DECEMBER), months.first())
        assertEquals(YearMonth(2026, Month.MAY), months.last())
    }

    @Test
    fun `reads the requested type and ignores the other one`() = runTest {
        val may = YearMonth(2026, Month.MAY)
        stubMonth(may, listOf(cat("SPEND", 900_00L)))
        stubIncomeMonth(may, listOf(cat("INCOME", 100_00L)))

        val result = useCase(TransactionType.Income, clock = fixedClock, zone = TimeZone.UTC, months = 1, topN = 3)

        // One query now returns both types; picking the wrong bucket would rank a user's salary
        // as a spending category.
        assertEquals(1, result.size)
        assertEquals(CategoryId("INCOME"), result.single().categoryId)
    }

    @Test
    fun `the uncategorized bucket never occupies a top-N slot`() = runTest {
        val may = YearMonth(2026, Month.MAY)
        stubMonth(may, listOf(uncategorized(900_00L), cat("A", 100_00L)))

        val result = useCase(TransactionType.Spend, clock = fixedClock, zone = TimeZone.UTC, months = 1, topN = 3)

        assertEquals(1, result.size)
        assertEquals(CategoryId("A"), result.single().categoryId)
    }

    @Test
    fun `empty months returns empty list`() = runTest {
        val result = useCase(TransactionType.Spend, clock = fixedClock, zone = TimeZone.UTC, months = 6, topN = 3)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `monthsInTop is counted correctly`() = runTest {
        val may = YearMonth(2026, Month.MAY)
        val apr = may.previous()

        stubMonth(may, listOf(cat("A", 500_00L), cat("B", 300_00L)))
        stubMonth(apr, listOf(cat("A", 400_00L)))

        val result = useCase(TransactionType.Spend, clock = fixedClock, zone = TimeZone.UTC, months = 2, topN = 3)

        val catA = result.first { it.categoryId == CategoryId("A") }
        val catB = result.first { it.categoryId == CategoryId("B") }

        assertEquals(2, catA.monthsInTop)
        assertEquals(1, catB.monthsInTop)
        assertEquals(2, catA.totalMonths)
    }

    @Test
    fun `partial data - only one month has data`() = runTest {
        val may = YearMonth(2026, Month.MAY)
        stubMonth(may, listOf(cat("A", 1000_00L)))

        val result = useCase(TransactionType.Spend, clock = fixedClock, zone = TimeZone.UTC, months = 6, topN = 3)

        assertEquals(1, result.size)
        assertEquals(Money(1000_00L), result[0].totalAmount)
        assertEquals(1, result[0].monthsInTop)
        assertEquals(6, result[0].totalMonths)
    }

    @Test
    fun `returns at most topN results`() = runTest {
        val may = YearMonth(2026, Month.MAY)
        stubMonth(may, listOf(cat("A", 500_00L), cat("B", 400_00L), cat("C", 300_00L), cat("D", 200_00L)))

        val result = useCase(TransactionType.Spend, clock = fixedClock, zone = TimeZone.UTC, months = 1, topN = 2)

        assertEquals(2, result.size)
    }
}
