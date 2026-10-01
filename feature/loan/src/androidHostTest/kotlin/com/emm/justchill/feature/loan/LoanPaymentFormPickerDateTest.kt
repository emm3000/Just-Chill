package com.emm.justchill.feature.loan

import com.emm.justchill.core.presentation.date.dateShortcutsOf
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

    @Test
    fun `the date sheet offers the shortcuts of the form's day`() {
        val form: LoanPaymentFormUi = paymentForm().copy(date = LocalDate(2026, 8, 10))

        assertEquals(dateShortcutsOf(form.today), form.dateShortcuts)
    }
}
