package com.emm.justchill.feature.loan

import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals

class LoanPaymentFormPickerDateTest {

    private val today: LocalDate = LocalDate(2026, 8, 22)

    private val untouchedForm: LoanPaymentFormUi = LoanPaymentFormUi(
        loanId = "loan-1",
        today = today,
        remainingCents = 16_000L,
    )

    @Test
    fun `the picker opens on today while no day is picked`() {
        assertEquals(today, untouchedForm.pickerDate)
    }

    @Test
    fun `the picker opens on the picked day once there is one`() {
        val pickedDay: LocalDate = LocalDate(2026, 8, 10)

        assertEquals(pickedDay, untouchedForm.copy(date = pickedDay).pickerDate)
    }
}
