package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.domain.transaction.TransactionWithCategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Every date here is fixed, including the reference "today" — it is an input to DayGroup, not an
// ambient clock read, so a run that crosses midnight cannot change any answer below.
class DayGroupTest {

    private val today = LocalDate(2026, 8, 10)

    private fun group(date: LocalDate, reference: LocalDate = today) =
        DayGroup(date = date, today = reference, transactions = emptyList())

    @Test fun primaryLabel_today_is_HOY() {
        assertEquals("HOY", group(today).primaryLabel)
    }

    @Test fun primaryLabel_yesterday_is_AYER() {
        assertEquals("AYER", group(LocalDate(2026, 8, 9)).primaryLabel)
    }

    @Test fun primaryLabel_yesterday_across_a_month_boundary_is_AYER() {
        assertEquals("AYER", group(LocalDate(2026, 7, 31), reference = LocalDate(2026, 8, 1)).primaryLabel)
    }

    @Test fun primaryLabel_tomorrow_is_not_HOY_nor_AYER() {
        assertEquals("Martes 11", group(LocalDate(2026, 8, 11)).primaryLabel)
    }

    @Test fun primaryLabel_regular_day_is_titlecase_weekday_plus_day_number() {
        assertEquals("Martes 13", group(LocalDate(2026, 1, 13)).primaryLabel)
        assertEquals("Miércoles 4", group(LocalDate(2026, 3, 4)).primaryLabel)
    }

    @Test fun monthYearCaption_is_lowercase_month_plus_year() {
        assertEquals("agosto 2026", group(LocalDate(2026, 8, 5)).monthYearCaption)
        assertEquals("diciembre 2025", group(LocalDate(2025, 12, 31)).monthYearCaption)
    }

    private val august: YearMonth = YearMonth(2026, Month.AUGUST)

    @Test fun `toDayGroups sums only the spend rows into spendTotal`() {
        val transactions: List<TransactionWithCategory> = listOf(
            transactionFixture("t-1", TransactionType.Spend, 500L, august, daysIntoMonth = 10),
            transactionFixture("t-2", TransactionType.Income, 10_000L, august, daysIntoMonth = 10),
            transactionFixture("t-3", TransactionType.Spend, 300L, august, daysIntoMonth = 10),
        )

        val groups: List<DayGroup> = transactions.toDayGroups(today)

        assertEquals(Money(800L), groups.single().spendTotal)
    }

    @Test fun `toDayGroups with no spend rows has no total`() {
        val transactions: List<TransactionWithCategory> =
            listOf(transactionFixture("t-1", TransactionType.Income, 10_000L, august, daysIntoMonth = 10))

        val groups: List<DayGroup> = transactions.toDayGroups(today)

        assertNull(groups.single().spendTotal)
    }
}
