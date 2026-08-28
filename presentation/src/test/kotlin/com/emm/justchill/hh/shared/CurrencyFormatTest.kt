package com.emm.justchill.hh.shared

import com.emm.domain.shared.Money
import org.junit.Test
import kotlin.test.assertEquals

class CurrencyFormatTest {

    @Test
    fun `a positive amount is positive money, signed plus`() {
        assertEquals("+S/ 500.00", Money(50_000L).positiveMoneyFormatted())
    }

    @Test
    fun `zero has no direction to point in, so it carries no sign`() {
        assertEquals("S/ 0.00", Money.Zero.positiveMoneyFormatted())
    }

    @Test
    fun `a negative amount keeps balanceFormatted's monochrome minus`() {
        assertEquals("−S/ 25.50", Money(-2_550L).positiveMoneyFormatted())
    }
}
