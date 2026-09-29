package com.emm.justchill.feature.loan

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoanDetailUiStateTest {

    private val firstPayment: LoanPaymentRowUi = paymentRow("pay-1", "S/ 300.00")
    private val secondPayment: LoanPaymentRowUi = paymentRow("pay-2", "S/ 200.00")

    private val listedPayments: LoanDetailUiState = LoanDetailUiState(payments = listOf(firstPayment, secondPayment))

    private fun paymentRow(id: String, amount: String): LoanPaymentRowUi = LoanPaymentRowUi(
        paymentId = id,
        amount = amount,
        methodLabel = "Efectivo",
        readablePaidAt = "15 de agosto de 2026",
        note = "",
    )

    @Test
    fun `the payment awaiting deletion is the listed one its id names`() {
        assertEquals(secondPayment, listedPayments.copy(pendingDeletePaymentId = "pay-2").pendingDeletePayment)
    }

    @Test
    fun `no payment awaits deletion while its id is unset or names none listed`() {
        assertNull(listedPayments.pendingDeletePayment)
        assertNull(listedPayments.copy(pendingDeletePaymentId = "gone").pendingDeletePayment)
    }
}
