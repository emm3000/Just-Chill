package com.emm.justchill.feature.report

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TotalAmountPartsTest {

    @Test
    fun `a formatted total splits into the currency prefix, the integer part and the decimals`() {
        assertEquals(
            TotalAmountParts(prefix = "S/ ", integer = "14", decimals = ".50"),
            splitTotalAmount("S/ 14.50"),
        )
    }

    @Test
    fun `the thousands separator stays in the integer part`() {
        assertEquals(
            TotalAmountParts(prefix = "S/ ", integer = "6,200", decimals = ".00"),
            splitTotalAmount("S/ 6,200.00"),
        )
    }

    @Test
    fun `a total without the currency prefix does not split`() {
        assertNull(splitTotalAmount("6,200.00"))
    }

    @Test
    fun `a total without decimals does not split`() {
        assertNull(splitTotalAmount("S/ 517"))
    }
}
