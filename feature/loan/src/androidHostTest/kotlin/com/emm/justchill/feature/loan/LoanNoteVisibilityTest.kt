package com.emm.justchill.feature.loan

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoanNoteVisibilityTest {

    @Test
    fun `a loan with written words has a note`() {
        assertTrue(loanSummary(note = "Para el arreglo del carro").hasNote)
    }

    @Test
    fun `a loan whose note is empty or only whitespace has none`() {
        assertFalse(loanSummary(note = "").hasNote)
        assertFalse(loanSummary(note = "  \n ").hasNote)
    }

    @Test
    fun `a payment with written words has a note`() {
        assertTrue(paymentRow(note = "Primer abono").hasNote)
    }

    @Test
    fun `a payment whose note is empty or only whitespace has none`() {
        assertFalse(paymentRow(note = "").hasNote)
        assertFalse(paymentRow(note = " \t ").hasNote)
    }
}
