package com.emm.justchill.core.presentation.format

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpanishSearchTest {

    @Test
    fun `an accented text matches its unaccented query with trailing space`() {
        assertTrue("Café".matchesSearch("cafe "))
    }

    @Test
    fun `a text matches an uppercase padded query it contains`() {
        assertTrue("Supermercado".matchesSearch("  SUPER "))
    }

    @Test
    fun `any text matches a blank query`() {
        assertTrue("Taxi".matchesSearch("   "))
        assertTrue("Taxi".matchesSearch(""))
    }

    @Test
    fun `a text does not match a query it does not contain`() {
        assertFalse("Taxi".matchesSearch("café"))
    }
}
