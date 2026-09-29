package com.emm.justchill.feature.loan

import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals

class LoanPaymentFormPickerDateTest {

    @Test
    fun `the picker opens on today while no day is picked`() {
        assertEquals(paymentForm().today, paymentForm().pickerDate)
    }

    @Test
    fun `the picker opens on the picked day once there is one`() {
        val pickedDay: LocalDate = LocalDate(2026, 8, 10)

        assertEquals(pickedDay, paymentForm().copy(date = pickedDay).pickerDate)
    }
}
