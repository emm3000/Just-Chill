package com.emm.domain.loan

import org.junit.Test
import kotlin.test.assertEquals

class PersonKeyTest {

    @Test
    fun `an accented Peruvian surname keys the same as its plain spelling`() {
        assertEquals(personKey("Juan Pérez"), personKey("juan perez"))
    }

    @Test
    fun `ñ folds to n so Muñoz and Munoz key the same`() {
        assertEquals(personKey("Muñoz"), personKey("Munoz"))
    }

    @Test
    fun `internal whitespace runs collapse to a single space`() {
        assertEquals("juan perez", personKey("Juan   Pérez"))
    }

    @Test
    fun `leading and trailing whitespace is trimmed`() {
        assertEquals("juan perez", personKey("  Juan Pérez  "))
    }

    @Test
    fun `a decomposed NFD surname keys the same as its precomposed and unaccented spellings`() {
        // "Pérez" as NFD: "e" followed by a standalone combining acute accent (U+0301), the shape
        // contacts pasted from macOS/iCloud arrive in.
        val decomposed = "Pérez"

        assertEquals(personKey("Pérez"), personKey(decomposed))
        assertEquals(personKey("Perez"), personKey(decomposed))
    }
}
