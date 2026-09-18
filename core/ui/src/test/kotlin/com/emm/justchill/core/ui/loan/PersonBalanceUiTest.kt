package com.emm.justchill.core.ui.loan

import com.emm.justchill.core.domain.loan.PersonBalance
import com.emm.justchill.core.domain.shared.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonBalanceUiTest {

    private fun balance(remaining: Long, personKey: String = "ana", personName: String = "Ana") = PersonBalance(
        personKey = personKey,
        personName = personName,
        remaining = Money(remaining),
    )

    @Test
    fun `a zero remaining is settled`() {
        assertTrue(listOf(balance(remaining = 0L)).toUi().single().isSettled)
    }

    @Test
    fun `a nonzero remaining is not settled`() {
        assertFalse(listOf(balance(remaining = 100L)).toUi().single().isSettled)
    }

    @Test
    fun `two balances that offset to zero total, owingNames still names the positive half`() {
        val balances = listOf(
            balance(remaining = 50_000L, personKey = "carlos", personName = "Carlos"),
            balance(remaining = -50_000L, personKey = "diego", personName = "Diego"),
        )

        assertFalse(balances.totalOwedIsPositive())
        assertEquals("S/ 0.00", balances.totalOwedFormatted())
        assertEquals(listOf("Carlos"), balances.owingNames())
    }
}
