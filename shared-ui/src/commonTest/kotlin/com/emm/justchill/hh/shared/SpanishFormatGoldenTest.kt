package com.emm.justchill.hh.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden tests pinning the hand-rolled Spanish formatters to the exact strings the old JVM
 * `java.time` / `java.text` localized formatters produced on the build JDK (captured empirically
 * for the `es` / `es-PE` locales). If any of these drift, the Spanish UX has changed.
 */
class SpanishFormatGoldenTest {

    private val jun13 = LocalDate(2026, Month.JUNE, 13)

    // ---- SpanishDateFormat ----

    @Test fun longDate_matches_FormatStyle_LONG() {
        // ofLocalizedDate(LONG, es) -> "13 de junio de 2026"
        assertEquals("13 de junio de 2026", SpanishDateFormat.longDate(jun13))
    }

    @Test fun dayShortMonth_matches_d_MMM() {
        // ofPattern("d MMM", es) -> "13 jun"
        assertEquals("13 jun", SpanishDateFormat.dayShortMonth(jun13))
    }

    @Test fun monthDayPadded_matches_MMMM_dd() {
        // ofPattern("MMMM dd", es) -> "junio 13" (zero-padded day)
        assertEquals("junio 13", SpanishDateFormat.monthDayPadded(jun13))
        assertEquals("junio 05", SpanishDateFormat.monthDayPadded(LocalDate(2026, Month.JUNE, 5)))
    }

    @Test fun monthYear_matches_MMMM_yyyy() {
        // ofPattern("MMMM yyyy", es) -> "septiembre 2026"
        assertEquals("septiembre 2026", SpanishDateFormat.monthYear(2026, Month.SEPTEMBER))
    }

    @Test fun dayFullMonth_matches_d_MMMM() {
        // ofPattern("d MMMM", es) -> "13 junio"
        assertEquals("13 junio", SpanishDateFormat.dayFullMonth(jun13))
    }

    @Test fun shortMonth_september_is_sept() {
        // The JVM `es` short month for September is "sept" (4 chars), not "sep".
        assertEquals("sept", SpanishDateFormat.shortMonth(Month.SEPTEMBER))
        assertEquals("ene", SpanishDateFormat.shortMonth(Month.JANUARY))
        assertEquals("dic", SpanishDateFormat.shortMonth(Month.DECEMBER))
    }

    @Test fun fullWeekday_matches_DayOfWeek_FULL() {
        // ISO 1..7 = Monday..Sunday
        assertEquals("lunes", SpanishDateFormat.fullWeekday(1))
        assertEquals("miércoles", SpanishDateFormat.fullWeekday(3))
        assertEquals("sábado", SpanishDateFormat.fullWeekday(6))
        assertEquals("domingo", SpanishDateFormat.fullWeekday(7))
    }

    @Test fun readableTime_matches_h_mm_a() {
        // ofPattern("h:mm a", es), 12-hour, "a. m." / "p. m."
        assertEquals("3:45 p. m.", SpanishDateFormat.readableTime(15, 45))
        assertEquals("9:05 a. m.", SpanishDateFormat.readableTime(9, 5))
        assertEquals("12:00 a. m.", SpanishDateFormat.readableTime(0, 0))
        assertEquals("12:00 p. m.", SpanishDateFormat.readableTime(12, 0))
        assertEquals("11:59 p. m.", SpanishDateFormat.readableTime(23, 59))
    }

    @Test fun titlecaseFirstChar_only_touches_first_char() {
        assertEquals("13 jun", "13 jun".titlecaseFirstChar()) // leading digit unchanged
        assertEquals("Septiembre 2026", "septiembre 2026".titlecaseFirstChar())
        assertEquals("", "".titlecaseFirstChar())
    }

    // ---- NumberFormatEs ----

    @Test fun decimal2_matches_DecimalFormat_and_esPE_NumberFormat() {
        assertEquals("0.00", NumberFormatEs.decimal2(0.0))
        assertEquals("0.01", NumberFormatEs.decimal2(0.01))
        assertEquals("1.00", NumberFormatEs.decimal2(1.0))
        assertEquals("1,234.56", NumberFormatEs.decimal2(1234.56))
        assertEquals("1,234,567.50", NumberFormatEs.decimal2(1234567.5))
        assertEquals("1,000,000.00", NumberFormatEs.decimal2(1000000.0))
        assertEquals("99,999,999,999.99", NumberFormatEs.decimal2(99999999999.99))
    }

    @Test fun integer_matches_esPE_IntegerInstance() {
        assertEquals("0", NumberFormatEs.integer(0))
        assertEquals("999", NumberFormatEs.integer(999))
        assertEquals("1,000", NumberFormatEs.integer(1000))
        assertEquals("1,234,567", NumberFormatEs.integer(1234567))
    }

    // ---- SpanishSearch ----

    @Test fun stripSpanishAccents_maps_accented_to_base() {
        assertEquals("cafe", "café".stripSpanishAccents())
        assertEquals("nino", "niño".stripSpanishAccents())
        assertEquals("aeiou", "áéíóú".stripSpanishAccents())
        assertEquals("AEIOUUN", "ÁÉÍÓÚÜÑ".stripSpanishAccents())
        assertEquals("pinguino", "pingüino".stripSpanishAccents())
    }
}
