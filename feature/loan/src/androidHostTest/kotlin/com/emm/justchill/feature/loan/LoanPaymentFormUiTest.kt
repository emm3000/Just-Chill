package com.emm.justchill.feature.loan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoanPaymentFormUiTest {

    @Test fun an_amount_under_what_remains_is_savable() {
        assertTrue(paymentForm("5000").isSaveEnabled)
        assertNull(paymentForm("5000").amountError)
    }

    // The domain admits the abono that settles the loan exactly, so the CTA has to as well.
    @Test fun an_amount_that_settles_what_remains_exactly_is_savable() {
        assertTrue(paymentForm("16000").isSaveEnabled)
        assertNull(paymentForm("16000").amountError)
    }

    @Test fun one_cent_past_what_remains_is_refused_in_the_words_the_use_case_would_have_used() {
        assertFalse(paymentForm("16001").isSaveEnabled)
        assertEquals("El abono es mayor que lo que falta pagar", paymentForm("16001").amountError)
    }

    @Test fun an_empty_amount_is_not_savable_and_reads_as_untyped_rather_than_over_the_cap() {
        assertFalse(paymentForm("").isSaveEnabled)
        assertNull(paymentForm("").amountError)
    }

    @Test fun an_amount_of_only_zeros_is_not_savable_and_does_not_read_as_over_the_cap() {
        assertFalse(paymentForm("000").isSaveEnabled)
        assertNull(paymentForm("000").amountError)
    }

    // Neither can be produced through sanitizeCentsInput, and neither is an amount, so the form
    // refuses both without claiming they are over the cap — it cannot know that.
    @Test fun an_amount_too_long_for_Long_is_not_savable_and_claims_nothing_about_the_cap() {
        assertFalse(paymentForm("9".repeat(25)).isSaveEnabled)
        assertNull(paymentForm("9".repeat(25)).amountError)
    }

    @Test fun a_negative_amount_is_not_savable_and_claims_nothing_about_the_cap() {
        assertFalse(paymentForm("-5").isSaveEnabled)
        assertNull(paymentForm("-5").amountError)
    }

    @Test fun a_settled_loan_leaves_nothing_savable() {
        assertFalse(paymentForm("1", remainingCents = 0L).isSaveEnabled)
        assertEquals("El abono es mayor que lo que falta pagar", paymentForm("1", remainingCents = 0L).amountError)
    }
}
