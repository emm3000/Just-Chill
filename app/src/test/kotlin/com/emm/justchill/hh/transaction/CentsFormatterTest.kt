package com.emm.justchill.hh.transaction

import com.emm.domain.shared.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class CentsFormatterTest {

    // sanitizeCentsInput
    @Test fun `sanitize keeps only digits`() {
        assertEquals("125050", sanitizeCentsInput("1,250.50"))
        assertEquals("100", sanitizeCentsInput("S/ 1.00"))
        assertEquals("", sanitizeCentsInput("abc"))
    }

    @Test fun `sanitize caps at MAX_AMOUNT_DIGITS`() {
        val twentyDigits = "1".repeat(20)
        assertEquals(13, sanitizeCentsInput(twentyDigits).length)
    }

    @Test fun `sanitize empty input`() {
        assertEquals("", sanitizeCentsInput(""))
    }

    // formatCentsForDisplay
    @Test fun `format empty digits as zero`() {
        assertEquals("0.00", formatCentsForDisplay(""))
    }

    @Test fun `format single cent`() {
        assertEquals("0.01", formatCentsForDisplay("1"))
    }

    @Test fun `format single sol`() {
        assertEquals("1.00", formatCentsForDisplay("100"))
    }

    @Test fun `format thousands with separator`() {
        assertEquals("1,250.50", formatCentsForDisplay("125050"))
    }

    @Test fun `format millions`() {
        assertEquals("1,000,000.00", formatCentsForDisplay("100000000"))
    }

    @Test fun `format leading zeros`() {
        assertEquals("1.25", formatCentsForDisplay("000125"))
    }

    @Test fun `format at max digits does not crash`() {
        val maxDigits = "9".repeat(13)
        // Should not throw; verify it's a Long
        val result = formatCentsForDisplay(maxDigits)
        assertEquals("99,999,999,999.99", result)
    }

    // centsToSoles (kept for validity checks in ViewModels)
    @Test fun `centsToSoles converts correctly`() {
        assertEquals(0.0, centsToSoles(""), 0.0001)
        assertEquals(0.01, centsToSoles("1"), 0.0001)
        assertEquals(1250.50, centsToSoles("125050"), 0.0001)
    }

    // centsToMoney
    @Test fun `centsToMoney converts correctly`() {
        assertEquals(Money(0L), centsToMoney(""))
        assertEquals(Money(1L), centsToMoney("1"))
        assertEquals(Money(125050L), centsToMoney("125050"))
    }

    // moneyCentsString
    @Test fun `moneyCentsString converts correctly`() {
        assertEquals("0", moneyCentsString(Money(0L)))
        assertEquals("1", moneyCentsString(Money(1L)))
        assertEquals("125050", moneyCentsString(Money(125050L)))
    }

    // Round-trip: cents string → Money → back to cents string
    @Test fun `round trip cents string to money and back`() {
        listOf("1", "100", "125050", "99999999").forEach { original ->
            val money = centsToMoney(original)
            val back = moneyCentsString(money)
            assertEquals(original, back)
        }
    }
}
