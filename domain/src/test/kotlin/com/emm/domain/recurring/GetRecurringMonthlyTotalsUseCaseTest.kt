package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.transaction.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetRecurringMonthlyTotalsUseCase — spec scenarios R4.1–R4.6 + R9.3.
 *
 * The use case is a pure calculator over List<RecurringMovementDetails>; no repository,
 * no coroutines required. Tests pass the list directly and assert synchronously.
 */
class GetRecurringMonthlyTotalsUseCaseTest {

    private lateinit var useCase: GetRecurringMonthlyTotalsUseCase

    @Before
    fun setUp() {
        useCase = GetRecurringMonthlyTotalsUseCase()
    }

    private fun detail(id: String, type: TransactionType, amount: Money? = Money(10_000L), isActive: Boolean = true) =
        RecurringMovementDetails(
            id = id,
            name = id,
            type = type,
            amount = amount,
            categoryName = null,
            categoryColor = null,
            accountName = "FakeAccount",
            dayOfMonth = 1,
            isActive = isActive,
        )

    // Kept for reference — original helper that built RecurringMovement objects;
    // replaced by detail() now that the use case accepts a list directly.
    @Suppress("unused")
    private fun rm(id: String, type: TransactionType, amount: Money? = Money(10_000L), isActive: Boolean = true) =
        RecurringMovement(
            id = RecurringMovementId(id),
            name = id,
            type = type,
            amount = amount,
            description = "",
            categoryId = null,
            accountId = AccountId("acc-1"),
            frequency = Frequency.Monthly,
            dayOfMonth = 1,
            isActive = isActive,
            lastConfirmedPeriod = null,
        )

    // ---- R4.1 — Mixed active fixed + variable templates ----

    @Test
    fun `R4_1 mixed active fixed and variable totals correct`() {
        val list = listOf(
            detail("i1", TransactionType.Income, Money(50_000L)),
            detail("s1", TransactionType.Spend, Money(20_000L)),
            detail("iv1", TransactionType.Income, amount = null),
            detail("sv1", TransactionType.Spend, amount = null),
        )

        val result = useCase(list)

        assertEquals(Money(50_000L), result.incomeTotal)
        assertEquals(Money(20_000L), result.expenseTotal)
        assertEquals(2, result.activeVariableCount)
    }

    // ---- R4.2 — All variable active templates ----

    @Test
    fun `R4_2 all variable active yields zero totals and correct count`() {
        val list = listOf(
            detail("iv1", TransactionType.Income, amount = null),
            detail("sv1", TransactionType.Spend, amount = null),
        )

        val result = useCase(list)

        assertEquals(Money(0L), result.incomeTotal)
        assertEquals(Money(0L), result.expenseTotal)
        assertEquals(2, result.activeVariableCount)
    }

    // ---- R4.3 — All paused templates ----

    @Test
    fun `R4_3 all paused templates excluded from all aggregations`() {
        val list = listOf(
            detail("i1", TransactionType.Income, Money(10_000L), isActive = false),
            detail("s1", TransactionType.Spend, Money(5_000L), isActive = false),
        )

        val result = useCase(list)

        assertEquals(Money(0L), result.incomeTotal)
        assertEquals(Money(0L), result.expenseTotal)
        assertEquals(0, result.activeVariableCount)
    }

    // ---- R4.4 — Empty template list ----

    @Test
    fun `R4_4 empty list returns RecurringMonthlyTotals Empty`() {
        val result = useCase(emptyList())

        assertEquals(RecurringMonthlyTotals.Empty, result)
    }

    // ---- R4.5 — Mixed active and paused (only active counted) ----

    @Test
    fun `R4_5 only active templates contribute to totals`() {
        val list = listOf(
            detail("i1", TransactionType.Income, Money(30_000L)), // active → counted
            detail("i2", TransactionType.Income, Money(10_000L), isActive = false), // paused → excluded
            detail("sv1", TransactionType.Spend, amount = null), // active variable → variableCount
            detail("s2", TransactionType.Spend, Money(20_000L), isActive = false), // paused → excluded
        )

        val result = useCase(list)

        assertEquals(Money(30_000L), result.incomeTotal)
        assertEquals(Money(0L), result.expenseTotal)
        assertEquals(1, result.activeVariableCount)
    }

    // ---- R4.6 — Multiple active fixed income templates summed correctly ----

    @Test
    fun `R4_6 multiple active income templates summed correctly`() {
        val list = listOf(
            detail("i1", TransactionType.Income, Money(100_000L)), // S/1000.00
            detail("i2", TransactionType.Income, Money(50_050L)), // S/500.50
            detail("i3", TransactionType.Income, Money(25_025L)), // S/250.25
        )

        val result = useCase(list)

        // 100000 + 50050 + 25025 = 175075
        assertEquals(Money(175_075L), result.incomeTotal)
        assertEquals(Money(0L), result.expenseTotal)
        assertEquals(0, result.activeVariableCount)
    }

    // ---- R9.3 — Paused variable templates NOT counted in variableCount ----

    @Test
    fun `R9_3 paused variable templates are not counted in activeVariableCount`() {
        val list = listOf(
            detail("av1", TransactionType.Income, amount = null), // active → counted
            detail("pv1", TransactionType.Spend, amount = null, isActive = false), // paused → excluded
            detail("pv2", TransactionType.Spend, amount = null, isActive = false), // paused → excluded
            detail("pv3", TransactionType.Income, amount = null, isActive = false), // paused → excluded
        )

        val result = useCase(list)

        assertEquals(1, result.activeVariableCount)
        assertEquals(Money(0L), result.incomeTotal)
        assertEquals(Money(0L), result.expenseTotal)
    }
}
