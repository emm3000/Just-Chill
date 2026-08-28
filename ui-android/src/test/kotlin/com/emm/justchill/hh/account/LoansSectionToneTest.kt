package com.emm.justchill.hh.account

import com.emm.justchill.core.ui.atoms.AmountTone
import kotlin.test.Test
import kotlin.test.assertEquals

class LoansSectionToneTest {

    @Test
    fun `a positive total owed takes success`() {
        assertEquals(AmountTone.Pos, loansTotalTone(totalOwedIsPositive = true))
    }

    @Test
    fun `a non-positive total is muted, however the people list reads`() {
        assertEquals(AmountTone.Mute, loansTotalTone(totalOwedIsPositive = false))
    }
}
