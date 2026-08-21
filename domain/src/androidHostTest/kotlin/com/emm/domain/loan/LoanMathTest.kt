package com.emm.domain.loan

import com.emm.domain.shared.Money
import org.junit.Test
import kotlin.test.assertEquals

class LoanMathTest {

    @Test
    fun `totalDue with zero interest returns the principal untouched`() {
        val totalDue = totalDue(principal = Money(50_000L), interestBps = 0)

        assertEquals(Money(50_000L), totalDue)
    }

    @Test
    fun `totalDue rounds an exact half cent up, disagreeing with HALF-EVEN`() {
        // raw interest is 2 cents * 2500 bps / 10_000 = 0.5 cents exactly.
        val totalDue = totalDue(principal = Money(2L), interestBps = 2500)

        assertEquals(Money(3L), totalDue)
    }

    @Test
    fun `totalDue rounds up a fraction that plain integer division would truncate away`() {
        // raw interest is 3 cents * 2000 bps / 10_000 = 0.6 cents; plain `/ 10_000` truncates to 0.
        val totalDue = totalDue(principal = Money(3L), interestBps = 2000)

        assertEquals(Money(4L), totalDue)
    }

    @Test
    fun `remaining is totalDue minus what has been paid`() {
        val remaining = remaining(totalDue = Money(1_000L), paidSoFar = Money(400L))

        assertEquals(Money(600L), remaining)
    }

    @Test
    fun `remaining is zero once payments cover totalDue exactly`() {
        val remaining = remaining(totalDue = Money(1_000L), paidSoFar = Money(1_000L))

        assertEquals(Money.Zero, remaining)
    }
}
