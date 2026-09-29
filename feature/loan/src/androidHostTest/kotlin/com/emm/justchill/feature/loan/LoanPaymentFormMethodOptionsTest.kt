package com.emm.justchill.feature.loan

import com.emm.justchill.core.domain.loan.PaymentMethod
import org.junit.Test
import kotlin.test.assertEquals

class LoanPaymentFormMethodOptionsTest {

    @Test
    fun `every method is offered in its Spanish label`() {
        val offered: List<Pair<PaymentMethod, String>> = paymentForm().methodOptions.map { it.method to it.label }

        assertEquals(listOf(PaymentMethod.Cash to "Efectivo", PaymentMethod.Transfer to "Transferencia"), offered)
    }

    @Test
    fun `only the chosen method reads as selected`() {
        val selected: List<PaymentMethod> = paymentForm().copy(method = PaymentMethod.Transfer).methodOptions
            .filter { it.isSelected }
            .map { it.method }

        assertEquals(listOf(PaymentMethod.Transfer), selected)
    }
}
