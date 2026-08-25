package com.emm.justchill.hh.recurring

import com.emm.domain.recurring.RecurringMovementDetails
import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecurringMovementUiTest {

    private fun template(
        type: TransactionType = TransactionType.Spend,
        amount: Money? = null,
        accountName: String? = "Cuenta sueldo",
    ) = RecurringMovementDetails(
        id = "template-1",
        name = "Netflix",
        type = type,
        amount = amount,
        categoryName = "Entretenimiento",
        categoryColor = "blue",
        accountName = accountName,
        dayOfMonth = 15,
        isActive = true,
    )

    @Test fun a_fixed_income_amount_formats_the_amount() {
        val ui = template(type = TransactionType.Income, amount = Money(150000)).toRecurringMovementUi()

        assertFalse(ui.isVariableAmount)
        assertEquals("+S/ 1,500.00", ui.formattedAmount)
    }

    @Test fun a_fixed_expense_amount_formats_the_amount() {
        val ui = template(type = TransactionType.Spend, amount = Money(150000)).toRecurringMovementUi()

        assertFalse(ui.isVariableAmount)
        assertEquals("−S/ 1,500.00", ui.formattedAmount)
    }

    @Test fun a_null_amount_is_the_modelled_variable_state_not_a_crash() {
        val ui = template(amount = null).toRecurringMovementUi()

        assertTrue(ui.isVariableAmount)
        assertEquals("Variable", ui.formattedAmount)
    }

    @Test fun a_missing_account_name_becomes_empty_not_null() {
        val ui = template(accountName = null).toRecurringMovementUi()

        assertEquals("", ui.accountName)
    }
}
