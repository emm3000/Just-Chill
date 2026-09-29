package com.emm.justchill.feature.report

import kotlin.test.Test
import kotlin.test.assertEquals

class ComparisonPercentLabelTest {

    @Test
    fun `renders the percent up to 999 as it is`() {
        assertEquals("999%", comparisonPercentLabel(999))
    }

    @Test
    fun `caps a percent above 999 at the more-than label`() {
        assertEquals("más de 999%", comparisonPercentLabel(1000))
    }

    @Test
    fun `caps a five-digit percent from a tiny base`() {
        assertEquals("más de 999%", comparisonPercentLabel(44499))
    }
}
