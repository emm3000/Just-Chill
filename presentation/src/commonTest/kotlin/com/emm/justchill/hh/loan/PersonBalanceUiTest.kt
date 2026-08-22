package com.emm.justchill.hh.loan

import com.emm.domain.loan.PersonBalance
import com.emm.domain.shared.Money
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonBalanceUiTest {

    private fun balance(remaining: Long) = PersonBalance(
        personKey = "ana",
        personName = "Ana",
        remaining = Money(remaining),
    )

    @Test fun toUi_zero_remaining_is_settled() {
        assertTrue(listOf(balance(remaining = 0L)).toUi().single().isSettled)
    }

    @Test fun toUi_nonzero_remaining_is_not_settled() {
        assertFalse(listOf(balance(remaining = 100L)).toUi().single().isSettled)
    }
}
