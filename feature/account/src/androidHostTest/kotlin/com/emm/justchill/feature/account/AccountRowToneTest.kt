package com.emm.justchill.feature.account

import com.emm.justchill.core.ui.atoms.AmountTone
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountRowToneTest {

    @Test
    fun `a silent account is muted, whatever its empty net would sign`() {
        assertEquals(AmountTone.Mute, accountNetTone(movementCount = 0, netIsPositive = false))
    }

    @Test
    fun `an account whose income outweighs its spend takes success`() {
        assertEquals(AmountTone.Pos, accountNetTone(movementCount = 2, netIsPositive = true))
    }

    @Test
    fun `an account that spent more than it earned stays monochrome`() {
        assertEquals(AmountTone.Neutral, accountNetTone(movementCount = 1, netIsPositive = false))
    }
}
