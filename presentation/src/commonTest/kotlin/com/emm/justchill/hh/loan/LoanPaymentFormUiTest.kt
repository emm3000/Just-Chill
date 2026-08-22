package com.emm.justchill.hh.loan

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoanPaymentFormUiTest {

    private fun form(amountDigits: String, remainingCents: Long = 16_000L) = LoanPaymentFormUi(
        loanId = "loan-1",
        today = LocalDate(2026, 8, 22),
        remainingCents = remainingCents,
        amountDigits = amountDigits,
    )

    @Test fun an_amount_under_what_remains_is_savable() {
        assertTrue(form("5000").isSaveEnabled)
        assertFalse(form("5000").exceedsRemaining)
    }

    // The domain admits the abono that settles the loan exactly, so the CTA has to as well.
    @Test fun an_amount_that_settles_what_remains_exactly_is_savable() {
        assertTrue(form("16000").isSaveEnabled)
        assertFalse(form("16000").exceedsRemaining)
    }

    @Test fun one_cent_past_what_remains_is_not_savable() {
        assertFalse(form("16001").isSaveEnabled)
        assertTrue(form("16001").exceedsRemaining)
    }

    @Test fun an_empty_amount_is_not_savable_and_reads_as_untyped_rather_than_over_the_cap() {
        assertFalse(form("").isSaveEnabled)
        assertFalse(form("").exceedsRemaining)
    }

    @Test fun an_amount_of_only_zeros_is_not_savable_and_does_not_read_as_over_the_cap() {
        assertFalse(form("000").isSaveEnabled)
        assertFalse(form("000").exceedsRemaining)
    }

    @Test fun an_amount_too_long_for_Long_is_not_savable() {
        assertFalse(form("9".repeat(25)).isSaveEnabled)
    }

    @Test fun a_settled_loan_leaves_nothing_savable() {
        assertFalse(form("1", remainingCents = 0L).isSaveEnabled)
    }
}
