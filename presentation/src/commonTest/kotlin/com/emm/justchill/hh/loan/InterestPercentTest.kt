package com.emm.justchill.hh.loan

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
