package com.emm.justchill.hh.seetransactions

import com.emm.domain.shared.Money
import com.emm.justchill.core.ui.atoms.AmountTone
import kotlin.test.Test
import kotlin.test.assertEquals

class SeeTransactionsMonthSummaryToneTest {

    @Test
    fun `a positive month balance takes success`() {
        assertEquals(AmountTone.Pos, balanceTone(Money(7_500L)))
    }

    @Test
    fun `a zero balance stays this line's own monochrome`() {
        assertEquals(AmountTone.Neutral, balanceTone(Money.Zero))
    }

    @Test
    fun `a negative balance stays monochrome too`() {
        assertEquals(AmountTone.Neutral, balanceTone(Money(-2_550L)))
    }
}
