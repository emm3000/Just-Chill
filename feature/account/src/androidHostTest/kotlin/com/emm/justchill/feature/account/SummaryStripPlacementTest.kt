package com.emm.justchill.feature.account

import kotlin.test.Test
import kotlin.test.assertEquals

class SummaryStripPlacementTest {

    @Test
    fun `spent and income sit side by side while both columns, the divider and its gaps fit`() {
        assertEquals(
            BesideOrStacked.Beside,
            summaryStripPlacement(spentWidth = 150, dividerWidth = 2, incomeWidth = 100, gap = 24, maxWidth = 320),
        )
    }

    @Test
    fun `income drops under spent once the strip overflows the width`() {
        assertEquals(
            BesideOrStacked.Stacked,
            summaryStripPlacement(spentWidth = 171, dividerWidth = 2, incomeWidth = 100, gap = 24, maxWidth = 320),
        )
    }

    @Test
    fun `a strip that fills the width to the last pixel stays side by side`() {
        assertEquals(
            BesideOrStacked.Beside,
            summaryStripPlacement(spentWidth = 170, dividerWidth = 2, incomeWidth = 100, gap = 24, maxWidth = 320),
        )
    }
}
