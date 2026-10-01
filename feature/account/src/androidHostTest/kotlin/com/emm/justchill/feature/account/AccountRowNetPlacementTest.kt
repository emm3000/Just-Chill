package com.emm.justchill.feature.account

import kotlin.test.Test
import kotlin.test.assertEquals

class AccountRowNetPlacementTest {

    @Test
    fun `the net stays on the right while the one-line subtitle and the net fit side by side`() {
        assertEquals(
            BesideOrStacked.Beside,
            accountNetPlacement(subtitleWidth = 200, netWidth = 80, gap = 12, textsSpace = 300),
        )
    }

    @Test
    fun `the net drops under the texts once the one-line subtitle and the net overflow the row`() {
        assertEquals(
            BesideOrStacked.Stacked,
            accountNetPlacement(subtitleWidth = 209, netWidth = 80, gap = 12, textsSpace = 300),
        )
    }

    @Test
    fun `a subtitle and net that fill the row to the last pixel stay side by side`() {
        assertEquals(
            BesideOrStacked.Beside,
            accountNetPlacement(subtitleWidth = 208, netWidth = 80, gap = 12, textsSpace = 300),
        )
    }
}
