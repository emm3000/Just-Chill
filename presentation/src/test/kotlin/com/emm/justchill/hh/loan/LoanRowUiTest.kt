package com.emm.justchill.hh.loan

import com.emm.domain.loan.Loan
import com.emm.domain.loan.LoanBalance
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoanRowUiTest {

    private fun balance(totalDue: Long, paidSoFar: Long) = LoanBalance(
        loan = Loan(
            id = LoanId("loan-1"),
            personName = "Ana",
            personKey = "ana",
            principal = Money(totalDue),
            interestBps = 0,
            totalDue = Money(totalDue),
            note = "",
            lentAt = LocalDateTime.parse("2026-08-10T12:00:00"),
        ),
        paidSoFar = Money(paidSoFar),
    )

    @Test fun toUi_paid_in_full_is_settled() {
        assertTrue(listOf(balance(totalDue = 100_000L, paidSoFar = 100_000L)).toUi().single().isSettled)
    }

    @Test fun toUi_partially_paid_is_not_settled() {
        assertFalse(listOf(balance(totalDue = 100_000L, paidSoFar = 10_000L)).toUi().single().isSettled)
    }
}
