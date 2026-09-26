package com.emm.justchill.core.domain.recurring

import com.emm.justchill.core.domain.shared.YearMonth
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PeriodKeyTest {

    @Test
    fun `periodKey formats correctly as YYYY-MM`() {
        assertEquals("2026-02", periodKey(YearMonth(2026, Month.FEBRUARY)))
        assertEquals("2026-12", periodKey(YearMonth(2026, Month.DECEMBER)))
        assertEquals("2026-01", periodKey(YearMonth(2026, Month.JANUARY)))
    }

    @Test
    fun `parsePeriodKey round-trips a period key`() {
        assertEquals(YearMonth(2026, Month.FEBRUARY), parsePeriodKey("2026-02"))
        assertEquals(YearMonth(2026, Month.DECEMBER), parsePeriodKey("2026-12"))
    }

    @Test
    fun `parsePeriodKey returns null on malformed input`() {
        assertNull(parsePeriodKey("2026"))
        assertNull(parsePeriodKey("2026-13"))
        assertNull(parsePeriodKey("2026-00"))
        assertNull(parsePeriodKey("abcd-ef"))
        assertNull(parsePeriodKey(""))
    }

    @Test
    fun `parsePeriodKey rejects a year no periodKey could have written`() {
        assertNull(parsePeriodKey("12345-07"))
        assertNull(parsePeriodKey("0-07"))
        assertNull(parsePeriodKey("0000-07"))
        assertEquals(YearMonth(9999, Month.DECEMBER), parsePeriodKey("9999-12"))
    }

    @Test
    fun `parsePeriodKey rejects a four-character year periodKey would never have padded`() {
        assertNull(parsePeriodKey("0999-07"))
        assertNull(parsePeriodKey("0001-01"))
        assertNull(parsePeriodKey("+999-07"))
    }

    @Test
    fun `parsePeriodKey accepts the first year periodKey can write without padding`() {
        assertEquals(YearMonth(1000, Month.JANUARY), parsePeriodKey("1000-01"))
    }

    @Test
    fun `parsePeriodKey still accepts a month written without its leading zero`() {
        assertEquals(YearMonth(2026, Month.JULY), parsePeriodKey("2026-7"))
    }
}
