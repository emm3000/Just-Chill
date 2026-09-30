package com.emm.justchill.feature.report

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TotalAmountPartsTest {

    @Test
    fun `a formatted total splits into the currency prefix, the integer part and the decimals`() {
        assertEquals(
            TotalAmountParts(prefix = "S/\u00A0", integer = "14", decimals = ".50"),
            splitTotalAmount("S/\u00A014.50"),
        )
    }

    @Test
    fun `the thousands separator stays in the integer part`() {
        assertEquals(
            TotalAmountParts(prefix = "S/\u00A0", integer = "6,200", decimals = ".00"),
            splitTotalAmount("S/\u00A06,200.00"),
        )
    }

    @Test
    fun `a total without the currency prefix does not split`() {
        assertNull(splitTotalAmount("6,200.00"))
    }

    @Test
    fun `a total without decimals does not split`() {
        assertNull(splitTotalAmount("S/\u00A0517"))
    }
}
