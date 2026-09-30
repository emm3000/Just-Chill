package com.emm.justchill.feature.report

import com.emm.justchill.core.presentation.format.CURRENCY_PREFIX
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

    @Test
    fun `describes a rise with the verb Subió`() {
        assertEquals(
            "Subió ${CURRENCY_PREFIX}660, 12%",
            comparisonPillDescription("${CURRENCY_PREFIX}660", 12, directionUp = true),
        )
    }

    @Test
    fun `describes a drop with the verb Bajó`() {
        assertEquals(
            "Bajó ${CURRENCY_PREFIX}660, 66%",
            comparisonPillDescription("${CURRENCY_PREFIX}660", 66, directionUp = false),
        )
    }

    @Test
    fun `describes a capped percent through the label`() {
        assertEquals(
            "Subió ${CURRENCY_PREFIX}712, más de 999%",
            comparisonPillDescription("${CURRENCY_PREFIX}712", 44499, directionUp = true),
        )
    }

    @Test
    fun `describes a month with no direction as Sin cambio`() {
        assertEquals("Sin cambio, 0%", comparisonPillDescription("${CURRENCY_PREFIX}0", 0, directionUp = null))
    }
}
