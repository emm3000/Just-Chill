package com.emm.justchill.feature.loan

import com.emm.justchill.core.ui.atoms.AmountTone
import kotlin.test.Test
import kotlin.test.assertEquals

class LoansScreenToneTest {

    @Test
    fun `a positive remaining takes success, same as LoansSection's total`() {
        assertEquals(AmountTone.Pos, personRemainingTone(isSettled = false, remainingIsPositive = true))
    }

    @Test
    fun `a settled balance is the muted step, not success`() {
        assertEquals(AmountTone.Mute, personRemainingTone(isSettled = true, remainingIsPositive = false))
    }

    @Test
    fun `an unsettled, non-positive remaining stays monochrome, not muted`() {
        assertEquals(AmountTone.Neutral, personRemainingTone(isSettled = false, remainingIsPositive = false))
    }
}
