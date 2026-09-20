package com.emm.justchill.core.ui.format

import com.emm.justchill.core.domain.shared.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CentsFormatterTest {

    @Test
    fun `keeps only the digits of raw input`() {
        assertEquals("125050", sanitizeCentsInput("1,250.50"))
        assertEquals("100", sanitizeCentsInput("S/ 1.00"))
        assertEquals("", sanitizeCentsInput("abc"))
    }

    @Test
    fun `caps raw input at the nine digit ceiling`() {
        val twentyDigits: String = "1".repeat(20)
        assertEquals(9, sanitizeCentsInput(twentyDigits).length)
    }

    @Test
    fun `sanitizes empty input to empty`() {
        assertEquals("", sanitizeCentsInput(""))
    }

    @Test
    fun `displays empty digits as zero`() {
        assertEquals("0.00", formatCentsForDisplay(""))
    }

    @Test
    fun `displays a single cent`() {
        assertEquals("0.01", formatCentsForDisplay("1"))
    }

    @Test
    fun `displays a single sol`() {
        assertEquals("1.00", formatCentsForDisplay("100"))
    }

    @Test
    fun `separates thousands`() {
        assertEquals("1,250.50", formatCentsForDisplay("125050"))
    }

    @Test
    fun `separates millions`() {
        assertEquals("1,000,000.00", formatCentsForDisplay("100000000"))
    }

    @Test
    fun `drops leading zeros`() {
        assertEquals("1.25", formatCentsForDisplay("000125"))
    }

    @Test
    fun `displays the nine digit ceiling`() {
        val maxDigits: String = "9".repeat(9)
        val result: String = formatCentsForDisplay(maxDigits)
        assertEquals("9,999,999.99", result)
    }

    @Test
    fun `saves a positive count of cents`() {
        assertTrue("1".isSavableAmount())
        assertTrue("125050".isSavableAmount())
        assertTrue("000125".isSavableAmount())
    }

    @Test
    fun `refuses to save what has not been typed yet`() {
        assertFalse("".isSavableAmount())
        assertFalse("0".isSavableAmount())
        assertFalse("000".isSavableAmount())
    }

    @Test
    fun `refuses to save what is not a positive count of cents`() {
        assertFalse("-5".isSavableAmount())
        assertFalse("9".repeat(25).isSavableAmount())
        assertFalse("abc".isSavableAmount())
    }

    @Test
    fun `converts cents to soles`() {
        assertEquals(0.0, centsToSoles(""), 0.0001)
        assertEquals(0.01, centsToSoles("1"), 0.0001)
        assertEquals(1250.50, centsToSoles("125050"), 0.0001)
    }

    @Test
    fun `converts cents to money`() {
        assertEquals(Money(0L), centsToMoney(""))
        assertEquals(Money(1L), centsToMoney("1"))
        assertEquals(Money(125050L), centsToMoney("125050"))
    }

    @Test
    fun `renders money back as a cents string`() {
        assertEquals("0", moneyCentsString(Money(0L)))
        assertEquals("1", moneyCentsString(Money(1L)))
        assertEquals("125050", moneyCentsString(Money(125050L)))
    }

    @Test
    fun `round trips a cents string through money`() {
        listOf("1", "100", "125050", "99999999").forEach { original: String ->
            val money: Money = centsToMoney(original)
            val back: String = moneyCentsString(money)
            assertEquals(original, back)
        }
    }
}
