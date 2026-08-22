package com.emm.justchill.hh.loan

import com.emm.domain.loan.MAX_INTEREST_BPS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class InterestPercentTest {

    @Test fun percentTextToBps_blank_means_zero() {
        assertEquals(0, percentTextToBps(""))
    }

    @Test fun percentTextToBps_integer() {
        assertEquals(1200, percentTextToBps("12"))
    }

    @Test fun percentTextToBps_one_decimal() {
        assertEquals(1250, percentTextToBps("12.5"))
    }

    @Test fun percentTextToBps_two_decimals() {
        assertEquals(1234, percentTextToBps("12.34"))
    }

    @Test fun percentTextToBps_accepts_spanish_comma() {
        assertEquals(1250, percentTextToBps("12,5"))
    }

    @Test fun percentTextToBps_zero_percent_boundary() {
        assertEquals(0, percentTextToBps("0"))
    }

    @Test fun percentTextToBps_hundred_percent_boundary() {
        assertEquals(10_000, percentTextToBps("100"))
    }

    @Test fun percentTextToBps_out_of_range_is_not_clamped() {
        assertEquals(15_000, percentTextToBps("150"))
    }

    @Test fun percentTextToBps_beyond_two_decimals_is_truncated_not_rounded() {
        assertEquals(1234, percentTextToBps("12.349"))
    }

    @Test fun percentTextToBps_garbage_input_falls_back_to_zero() {
        assertEquals(0, percentTextToBps("abc"))
    }

    @Test fun percentTextToBps_overflowing_input_is_out_of_range_not_zero() {
        val result = percentTextToBps("12345678901234567890")
        assertNotEquals(0, result)
        assertTrue(result > MAX_INTEREST_BPS)
    }

    @Test fun bpsToPercentText_round_trips_two_decimals() {
        assertEquals(1234, percentTextToBps(bpsToPercentText(1234)))
    }

    @Test fun bpsToPercentText_round_trips_whole_percent() {
        assertEquals(1200, percentTextToBps(bpsToPercentText(1200)))
    }

    @Test fun bpsToPercentText_round_trips_zero_boundary() {
        assertEquals(0, percentTextToBps(bpsToPercentText(0)))
    }

    @Test fun bpsToPercentText_round_trips_hundred_percent_boundary() {
        assertEquals(10_000, percentTextToBps(bpsToPercentText(10_000)))
    }

    @Test fun sanitizeInterestPercentInput_drops_non_digit_non_separator_characters() {
        assertEquals("1", sanitizeInterestPercentInput("1o"))
    }

    @Test fun sanitizeInterestPercentInput_keeps_a_single_dot_separator() {
        assertEquals("12.5", sanitizeInterestPercentInput("12.5"))
    }

    @Test fun sanitizeInterestPercentInput_keeps_the_spanish_comma() {
        assertEquals("12,5", sanitizeInterestPercentInput("12,5"))
    }

    @Test fun sanitizeInterestPercentInput_drops_every_separator_after_the_first() {
        assertEquals("12.56", sanitizeInterestPercentInput("12.5.6"))
    }

    @Test fun sanitizeInterestPercentInput_keeps_exactly_two_decimals() {
        assertEquals("12.34", sanitizeInterestPercentInput("12.34"))
    }

    @Test fun sanitizeInterestPercentInput_refuses_a_third_decimal() {
        assertEquals("12.34", sanitizeInterestPercentInput("12.349"))
    }

    @Test fun sanitizeInterestPercentInput_keeps_a_trailing_separator() {
        assertEquals("12.", sanitizeInterestPercentInput("12."))
    }
}
