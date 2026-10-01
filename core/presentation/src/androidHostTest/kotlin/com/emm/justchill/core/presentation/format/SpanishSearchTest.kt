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

    @Test
    fun `an acute accent matches its plain vowel`() {
        assertTrue("café".matchesSearch("cafe"))
        assertTrue("cafe".matchesSearch("café"))
    }

    @Test
    fun `a tilde ñ matches a plain n`() {
        assertTrue("niño".matchesSearch("nino"))
        assertTrue("nino".matchesSearch("niño"))
    }

    @Test
    fun `every accented lowercase vowel matches its plain vowel`() {
        assertTrue("áéíóú".matchesSearch("aeiou"))
        assertTrue("aeiou".matchesSearch("áéíóú"))
    }

    @Test
    fun `uppercase accented letters and Ñ match their plain lowercase`() {
        assertTrue("ÁÉÍÓÚÜÑ".matchesSearch("aeiouun"))
        assertTrue("aeiouun".matchesSearch("ÁÉÍÓÚÜÑ"))
    }

    @Test
    fun `a diaeresis ü matches a plain u`() {
        assertTrue("pingüino".matchesSearch("pinguino"))
        assertTrue("pinguino".matchesSearch("pingüino"))
    }
}
