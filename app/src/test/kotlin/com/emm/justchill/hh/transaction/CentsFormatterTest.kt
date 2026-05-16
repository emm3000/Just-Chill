package com.emm.justchill.hh.transaction

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

    // centsToSoles
    @Test fun `centsToSoles converts correctly`() {
        assertEquals(0.0, centsToSoles(""), 0.0001)
        assertEquals(0.01, centsToSoles("1"), 0.0001)
        assertEquals(1250.50, centsToSoles("125050"), 0.0001)
    }

    // solesToCentsString
    @Test fun `solesToCentsString converts correctly`() {
        assertEquals("0", solesToCentsString(0.0))
        assertEquals("1", solesToCentsString(0.01))
        assertEquals("125050", solesToCentsString(1250.50))
    }

    // Round-trip
    @Test fun `round trip soles to cents and back`() {
        listOf(0.01, 1.0, 125.50, 999_999.99).forEach { original ->
            val digits = solesToCentsString(original)
            val back = centsToSoles(digits)
            assertEquals(original, back, 0.0001)
        }
    }
}
