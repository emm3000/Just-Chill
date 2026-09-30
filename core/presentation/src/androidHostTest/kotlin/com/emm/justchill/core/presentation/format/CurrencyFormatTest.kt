package com.emm.justchill.core.presentation.format

import com.emm.justchill.core.domain.shared.Money
import org.junit.Test
import kotlin.test.assertEquals

class CurrencyFormatTest {

    @Test
    fun `a positive amount is positive money, signed plus`() {
        assertEquals("+S/\u00A0500.00", Money(50_000L).positiveMoneyFormatted())
    }

    @Test
    fun `zero has no direction to point in, so it carries no sign`() {
        assertEquals("S/\u00A00.00", Money.Zero.positiveMoneyFormatted())
    }

    @Test
    fun `a negative amount keeps balanceFormatted's monochrome minus`() {
        assertEquals("−S/\u00A025.50", Money(-2_550L).positiveMoneyFormatted())
    }

    @Test
    fun `income joins the symbol and the amount with a non-breaking space`() {
        assertEquals("+S/\u00A01,200.00", formatIncome("1,200.00"))
    }

    @Test
    fun `expense joins the symbol and the amount with a non-breaking space`() {
        assertEquals("−S/\u00A025.50", formatExpense("25.50"))
    }

    @Test
    fun `neutral joins the symbol and the amount with a non-breaking space`() {
        assertEquals("S/\u00A0300.00", formatNeutral("300.00"))
    }
}
