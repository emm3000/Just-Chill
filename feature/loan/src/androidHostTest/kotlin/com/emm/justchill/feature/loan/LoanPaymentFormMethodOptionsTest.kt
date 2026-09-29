package com.emm.justchill.feature.loan

import com.emm.justchill.core.domain.loan.PaymentMethod
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals

class LoanPaymentFormMethodOptionsTest {

    private val cashForm: LoanPaymentFormUi = LoanPaymentFormUi(
        loanId = "loan-1",
        today = LocalDate(2026, 8, 22),
        remainingCents = 16_000L,
    )

    @Test
    fun `every method is offered in its Spanish label`() {
        val offered: List<Pair<PaymentMethod, String>> = cashForm.methodOptions.map { it.method to it.label }

        assertEquals(listOf(PaymentMethod.Cash to "Efectivo", PaymentMethod.Transfer to "Transferencia"), offered)
    }

    @Test
    fun `only the chosen method reads as selected`() {
        val selected: List<PaymentMethod> = cashForm.copy(method = PaymentMethod.Transfer).methodOptions
            .filter { it.isSelected }
            .map { it.method }

        assertEquals(listOf(PaymentMethod.Transfer), selected)
    }
}
