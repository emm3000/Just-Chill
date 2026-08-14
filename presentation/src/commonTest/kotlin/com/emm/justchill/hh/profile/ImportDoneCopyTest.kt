package com.emm.justchill.hh.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ImportDoneCopyTest {

    @Test
    fun `leaves the original sentence untouched when no recurring movement landed`() {
        assertEquals(
            "Listo — 3 movimientos importados.",
            buildImportDoneMessage(transactions = 3, recurring = 0),
        )
    }

    @Test
    fun `uses the singular for a single recurring movement`() {
        assertEquals(
            "Listo — 3 movimientos y 1 recurrente importados.",
            buildImportDoneMessage(transactions = 3, recurring = 1),
        )
    }

    @Test
    fun `names both counts when more than one recurring movement landed`() {
        assertEquals(
            "Listo — 3 movimientos y 5 recurrentes importados.",
            buildImportDoneMessage(transactions = 3, recurring = 5),
        )
    }

    @Test
    fun `uses the singular for a single movement`() {
        assertEquals(
            "Listo — 1 movimiento importado.",
            buildImportDoneMessage(transactions = 1, recurring = 0),
        )
    }

    @Test
    fun `uses both singulars when one movement and one recurring movement landed`() {
        // The only input where the two singular rules meet, and the one this suite was missing
        // while the transactions half had no singular at all: the sentence then showed a correct
        // "1 recurrente" next to a wrong "1 movimientos", one rule each, in one line of copy.
        // The participle stays plural — two singular subjects joined by "y" take it.
        assertEquals(
            "Listo — 1 movimiento y 1 recurrente importados.",
            buildImportDoneMessage(transactions = 1, recurring = 1),
        )
    }
}
