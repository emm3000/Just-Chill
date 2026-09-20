package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.TransactionId
import com.emm.justchill.core.domain.shared.YearMonth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals

class GetMonthSpendUseCaseTest {

    private val month = YearMonth(2026, Month.AUGUST)
    private val rows: MutableStateFlow<List<Transaction>> = MutableStateFlow(emptyList())

    private val transactionRepository = mockk<TransactionRepository> {
        every { allInRange(any(), any()) } returns rows
    }
    private val useCase = GetMonthSpendUseCase(transactionRepository)

    private fun movement(type: TransactionType, cents: Long): Transaction = Transaction(
        transactionId = TransactionId("tx-$cents-$type"),
        type = type,
        amount = Money(cents),
        description = "",
        occurredAt = LocalDateTime(2026, Month.AUGUST, 10, 12, 0),
        accountId = AccountId("acc-1"),
        categoryId = null,
    )

    @Test
    fun `reads the month's own day range`() = runTest {
        useCase(month).first()

        verify { transactionRepository.allInRange("2026-08-01", "2026-09-01") }
    }

    @Test
    fun `answers zero for a month with no movements`() = runTest {
        assertEquals(Money.Zero, useCase(month).first())
    }

    @Test
    fun `sums the spends and leaves the income out`() = runTest {
        rows.value = listOf(
            movement(TransactionType.Spend, 85_40L),
            movement(TransactionType.Income, 1_200_00L),
            movement(TransactionType.Spend, 14_60L),
        )

        assertEquals(Money(100_00L), useCase(month).first())
    }

    @Test
    fun `re-emits when the month's movements change`() = runTest {
        val totals: MutableList<Money> = mutableListOf()
        val spends: Flow<Money> = useCase(month)
        val job = launch { spends.collect(totals::add) }
        runCurrent()

        rows.value = listOf(movement(TransactionType.Spend, 85_40L))
        runCurrent()

        assertEquals(listOf(Money.Zero, Money(85_40L)), totals)
        job.cancel()
    }
}
