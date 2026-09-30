package com.emm.justchill.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ImportDoneCopyTest {

    @Test
    fun `leaves the original sentence untouched when nothing else landed`() {
        assertEquals(
            "Listo — 3 movimientos importados.",
            ProfileMessage.ImportDone(transactions = 3, recurring = 0, loans = 0, loanPayments = 0).toText(),
        )
    }

    @Test
    fun `uses the singular for a single recurring movement`() {
        assertEquals(
            "Listo — 3 movimientos y 1 recurrente importados.",
            ProfileMessage.ImportDone(transactions = 3, recurring = 1, loans = 0, loanPayments = 0).toText(),
        )
    }

    @Test
    fun `names both counts when more than one recurring movement landed`() {
        assertEquals(
            "Listo — 3 movimientos y 5 recurrentes importados.",
            ProfileMessage.ImportDone(transactions = 3, recurring = 5, loans = 0, loanPayments = 0).toText(),
        )
    }

    @Test
    fun `uses the singular for a single movement`() {
        assertEquals(
            "Listo — 1 movimiento importado.",
            ProfileMessage.ImportDone(transactions = 1, recurring = 0, loans = 0, loanPayments = 0).toText(),
        )
    }

    @Test
    fun `keeps the participle plural for two singular subjects joined by y`() {
        assertEquals(
            "Listo — 1 movimiento y 1 recurrente importados.",
            ProfileMessage.ImportDone(transactions = 1, recurring = 1, loans = 0, loanPayments = 0).toText(),
        )
    }

    @Test
    fun `names a single loan`() {
        assertEquals(
            "Listo — 2 movimientos y 1 préstamo importados.",
            ProfileMessage.ImportDone(transactions = 2, recurring = 0, loans = 1, loanPayments = 0).toText(),
        )
    }

    @Test
    fun `uses the plural for more than one loan`() {
        assertEquals(
            "Listo — 2 movimientos y 3 préstamos importados.",
            ProfileMessage.ImportDone(transactions = 2, recurring = 0, loans = 3, loanPayments = 0).toText(),
        )
    }

    @Test
    fun `names a single loan payment as one abono`() {
        assertEquals(
            "Listo — 2 movimientos y 1 abono importados.",
            ProfileMessage.ImportDone(transactions = 2, recurring = 0, loans = 0, loanPayments = 1).toText(),
        )
    }

    @Test
    fun `uses the plural for more than one loan payment`() {
        assertEquals(
            "Listo — 2 movimientos y 4 abonos importados.",
            ProfileMessage.ImportDone(transactions = 2, recurring = 0, loans = 0, loanPayments = 4).toText(),
        )
    }

    @Test
    fun `separates every landed clause with commas and joins the last one with y`() {
        assertEquals(
            "Listo — 2 movimientos, 1 recurrente, 1 préstamo y 1 abono importados.",
            ProfileMessage.ImportDone(transactions = 2, recurring = 1, loans = 1, loanPayments = 1).toText(),
        )
    }

    @Test
    fun `names every landed kind with its own count`() {
        assertEquals(
            "Listo — 2 movimientos, 1 recurrente, 3 préstamos y 4 abonos importados.",
            ProfileMessage.ImportDone(transactions = 2, recurring = 1, loans = 3, loanPayments = 4).toText(),
        )
    }
}
