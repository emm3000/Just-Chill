package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionFilterTest {

    @Test
    fun `a filter with no query, categories or amount bounds is empty`() {
        assertTrue(TransactionFilter.None.isEmpty)
    }

    @Test
    fun `a minimum amount alone makes the filter not empty`() {
        val filter: TransactionFilter = TransactionFilter.None.withAmountRange(
            minAmount = Money(2_000L),
            maxAmount = null,
        )

        assertFalse(filter.isEmpty)
    }

    @Test
    fun `a maximum amount alone makes the filter not empty`() {
        val filter: TransactionFilter = TransactionFilter.None.withAmountRange(
            minAmount = null,
            maxAmount = Money(5_000L),
        )

        assertFalse(filter.isEmpty)
    }

    @Test
    fun `min and max in order are kept as given`() {
        val filter: TransactionFilter = TransactionFilter.None.withAmountRange(
            minAmount = Money(2_000L),
            maxAmount = Money(5_000L),
        )

        assertEquals(Money(2_000L), filter.minAmount)
        assertEquals(Money(5_000L), filter.maxAmount)
    }

    @Test
    fun `min greater than max is swapped, never rejected`() {
        val filter: TransactionFilter = TransactionFilter.None.withAmountRange(
            minAmount = Money(5_000L),
            maxAmount = Money(2_000L),
        )

        assertEquals(Money(2_000L), filter.minAmount)
        assertEquals(Money(5_000L), filter.maxAmount)
    }

    @Test
    fun `equal bounds match one amount`() {
        val filter: TransactionFilter = TransactionFilter.None.withAmountRange(
            minAmount = Money(3_000L),
            maxAmount = Money(3_000L),
        )

        assertEquals(Money(3_000L), filter.minAmount)
        assertEquals(Money(3_000L), filter.maxAmount)
    }

    @Test
    fun `a one-sided minimum leaves the maximum untouched`() {
        val filter: TransactionFilter = TransactionFilter.None.withAmountRange(
            minAmount = Money(1_000L),
            maxAmount = null,
        )

        assertEquals(Money(1_000L), filter.minAmount)
        assertEquals(null, filter.maxAmount)
    }

    @Test
    fun `clearing both bounds returns to an unbounded filter`() {
        val ranged: TransactionFilter = TransactionFilter.None.withAmountRange(
            minAmount = Money(1_000L),
            maxAmount = Money(2_000L),
        )

        val cleared: TransactionFilter = ranged.withAmountRange(minAmount = null, maxAmount = null)

        assertTrue(cleared.isEmpty)
    }
}
