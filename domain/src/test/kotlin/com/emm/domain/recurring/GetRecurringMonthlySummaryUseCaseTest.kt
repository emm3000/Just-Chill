package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class GetRecurringMonthlySummaryUseCaseTest {

    private lateinit var repository: FakeRecurringMovementRepository
    private lateinit var useCase: GetRecurringMonthlySummaryUseCase

    @Before
    fun setUp() {
        repository = FakeRecurringMovementRepository()
        useCase = GetRecurringMonthlySummaryUseCase(repository, GetRecurringMonthlyTotalsUseCase())
    }

    private fun template(
        id: String,
        type: TransactionType,
        amount: Money? = Money(10_000L),
        isActive: Boolean = true,
    ) = RecurringMovement(
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
        createdAt = 0L,
    )

    @Test
    fun `no templates is zero of both`() = runTest {
        val summary = useCase().first()

        assertEquals(0, summary.activeCount)
        assertEquals(Money.Zero, summary.monthlyOutflow)
    }

    @Test
    fun `the count is every active template, income and expense alike`() = runTest {
        repository.addTemplate(
            template("s1", TransactionType.Spend),
            template("s2", TransactionType.Spend),
            template("i1", TransactionType.Income),
        )

        assertEquals(3, useCase().first().activeCount)
    }

    @Test
    fun `a paused template counts for neither the count nor the outflow`() = runTest {
        repository.addTemplate(
            template("s1", TransactionType.Spend, Money(9_000L)),
            template("s2", TransactionType.Spend, Money(5_000L), isActive = false),
        )

        val summary = useCase().first()

        assertEquals(1, summary.activeCount)
        assertEquals(Money(9_000L), summary.monthlyOutflow)
    }

    @Test
    fun `the outflow is the expense side only`() = runTest {
        repository.addTemplate(
            template("s1", TransactionType.Spend, Money(6_000L)),
            template("s2", TransactionType.Spend, Money(3_000L)),
            template("i1", TransactionType.Income, Money(500_000L)),
        )

        assertEquals(Money(9_000L), useCase().first().monthlyOutflow)
    }

    @Test
    fun `a variable-amount template is counted but adds nothing to the outflow`() = runTest {
        repository.addTemplate(
            template("s1", TransactionType.Spend, Money(9_000L)),
            template("s2", TransactionType.Spend, amount = null),
        )

        val summary = useCase().first()

        assertEquals(2, summary.activeCount)
        assertEquals(Money(9_000L), summary.monthlyOutflow)
    }
}
