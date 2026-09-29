package com.emm.justchill.feature.loan

import com.emm.justchill.core.presentation.loan.PersonBalanceUi
import org.junit.Test
import kotlin.test.assertEquals

class PersonRowToneTest {

    private fun toneOf(isSettled: Boolean, remainingIsPositive: Boolean): PersonRemainingTone = listOf(
        PersonBalanceUi(
            personKey = "ana",
            personName = "Ana",
            remaining = "S/ 0.00",
            isSettled = isSettled,
            remainingIsPositive = remainingIsPositive,
        ),
    ).toRows().single().tone

    @Test
    fun `a positive remaining takes success, same as LoansSection's total`() {
        assertEquals(PersonRemainingTone.Positive, toneOf(isSettled = false, remainingIsPositive = true))
    }

    @Test
    fun `a settled balance is the muted step, not success`() {
        assertEquals(PersonRemainingTone.Muted, toneOf(isSettled = true, remainingIsPositive = false))
    }

    @Test
    fun `an unsettled, non-positive remaining stays monochrome, not muted`() {
        assertEquals(PersonRemainingTone.Neutral, toneOf(isSettled = false, remainingIsPositive = false))
    }
}
