package com.emm.justchill.hh.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ImportDoneCopyTest {

    @Test
    fun `leaves the original sentence untouched when nothing else landed`() {
        assertEquals(
            "Listo — 3 movimientos importados.",
            buildImportDoneMessage(transactions = 3, recurring = 0, loans = 0, loanPayments = 0),
        )
    }

    @Test
    fun `uses the singular for a single recurring movement`() {
        assertEquals(
            "Listo — 3 movimientos y 1 recurrente importados.",
            buildImportDoneMessage(transactions = 3, recurring = 1, loans = 0, loanPayments = 0),
        )
    }

    @Test
    fun `names both counts when more than one recurring movement landed`() {
        assertEquals(
            "Listo — 3 movimientos y 5 recurrentes importados.",
            buildImportDoneMessage(transactions = 3, recurring = 5, loans = 0, loanPayments = 0),
        )
    }

    @Test
    fun `uses the singular for a single movement`() {
        assertEquals(
            "Listo — 1 movimiento importado.",
            buildImportDoneMessage(transactions = 1, recurring = 0, loans = 0, loanPayments = 0),
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
            buildImportDoneMessage(transactions = 1, recurring = 1, loans = 0, loanPayments = 0),
        )
    }

    @Test
    fun `names a single loan`() {
        assertEquals(
            "Listo — 2 movimientos y 1 préstamo importados.",
            buildImportDoneMessage(transactions = 2, recurring = 0, loans = 1, loanPayments = 0),
        )
    }

    @Test
    fun `uses the plural for more than one loan`() {
        assertEquals(
            "Listo — 2 movimientos y 3 préstamos importados.",
            buildImportDoneMessage(transactions = 2, recurring = 0, loans = 3, loanPayments = 0),
        )
    }

    @Test
    fun `names a single loan payment as one abono`() {
        assertEquals(
            "Listo — 2 movimientos y 1 abono importados.",
            buildImportDoneMessage(transactions = 2, recurring = 0, loans = 0, loanPayments = 1),
        )
    }

    @Test
    fun `uses the plural for more than one loan payment`() {
        assertEquals(
            "Listo — 2 movimientos y 4 abonos importados.",
            buildImportDoneMessage(transactions = 2, recurring = 0, loans = 0, loanPayments = 4),
        )
    }

    @Test
    fun `separates every landed clause with commas and joins the last one with y`() {
        assertEquals(
            "Listo — 2 movimientos, 1 recurrente, 1 préstamo y 1 abono importados.",
            buildImportDoneMessage(transactions = 2, recurring = 1, loans = 1, loanPayments = 1),
        )
    }
}
