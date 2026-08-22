package com.emm.justchill.hh.transaction

import com.emm.domain.shared.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CentsFormatterTest {

    // sanitizeCentsInput
    @Test fun sanitize_keeps_only_digits() {
        assertEquals("125050", sanitizeCentsInput("1,250.50"))
        assertEquals("100", sanitizeCentsInput("S/ 1.00"))
        assertEquals("", sanitizeCentsInput("abc"))
    }

    @Test fun sanitize_caps_at_MAX_AMOUNT_DIGITS() {
        val twentyDigits = "1".repeat(20)
        assertEquals(13, sanitizeCentsInput(twentyDigits).length)
    }

    @Test fun sanitize_empty_input() {
        assertEquals("", sanitizeCentsInput(""))
    }

    // formatCentsForDisplay
    @Test fun format_empty_digits_as_zero() {
        assertEquals("0.00", formatCentsForDisplay(""))
    }

    @Test fun format_single_cent() {
        assertEquals("0.01", formatCentsForDisplay("1"))
    }

    @Test fun format_single_sol() {
        assertEquals("1.00", formatCentsForDisplay("100"))
    }

    @Test fun format_thousands_with_separator() {
        assertEquals("1,250.50", formatCentsForDisplay("125050"))
    }

    @Test fun format_millions() {
        assertEquals("1,000,000.00", formatCentsForDisplay("100000000"))
    }

    @Test fun format_leading_zeros() {
        assertEquals("1.25", formatCentsForDisplay("000125"))
    }

    @Test fun format_at_max_digits_does_not_crash() {
        val maxDigits = "9".repeat(13)
        val result = formatCentsForDisplay(maxDigits)
        assertEquals("99,999,999,999.99", result)
    }

    // isSavableAmount
    @Test fun a_positive_count_of_cents_is_savable() {
        assertTrue("1".isSavableAmount())
        assertTrue("125050".isSavableAmount())
        assertTrue("000125".isSavableAmount())
    }

    @Test fun nothing_typed_yet_is_not_savable() {
        assertFalse("".isSavableAmount())
        assertFalse("0".isSavableAmount())
        assertFalse("000".isSavableAmount())
    }

    // sanitizeCentsInput strips the sign and caps the length, so neither reaches the forms from the
    // Android UI — but the intents carrying these strings are exported to Swift unsanitized.
    @Test fun a_string_that_is_not_a_positive_count_of_cents_is_not_savable() {
        assertFalse("-5".isSavableAmount())
        assertFalse("9".repeat(25).isSavableAmount())
        assertFalse("abc".isSavableAmount())
    }

    // centsToSoles
    @Test fun centsToSoles_converts_correctly() {
        assertEquals(0.0, centsToSoles(""), 0.0001)
        assertEquals(0.01, centsToSoles("1"), 0.0001)
        assertEquals(1250.50, centsToSoles("125050"), 0.0001)
    }

    // centsToMoney
    @Test fun centsToMoney_converts_correctly() {
        assertEquals(Money(0L), centsToMoney(""))
        assertEquals(Money(1L), centsToMoney("1"))
        assertEquals(Money(125050L), centsToMoney("125050"))
    }

    // moneyCentsString
    @Test fun moneyCentsString_converts_correctly() {
        assertEquals("0", moneyCentsString(Money(0L)))
        assertEquals("1", moneyCentsString(Money(1L)))
        assertEquals("125050", moneyCentsString(Money(125050L)))
    }

    // Round-trip: cents string -> Money -> back to cents string
    @Test fun round_trip_cents_string_to_money_and_back() {
        listOf("1", "100", "125050", "99999999").forEach { original ->
            val money = centsToMoney(original)
            val back = moneyCentsString(money)
            assertEquals(original, back)
        }
    }
}
