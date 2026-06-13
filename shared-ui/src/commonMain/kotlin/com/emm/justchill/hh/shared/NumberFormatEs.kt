package com.emm.justchill.hh.shared

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Hand-rolled grouped number formatting for Compose Multiplatform commonMain. Replaces the JVM
 * `java.text.DecimalFormat` / `NumberFormat` used by `CentsFormatter`, `MoneyFormatter`, and
 * `AmountHero`. The app is Spanish-only and, on the build JDK, both the US `DecimalFormat` and the
 * `es-PE` `NumberFormat` produce COMMA thousands separators with a DOT decimal point — so a single
 * hardcoded symbol set reproduces all three call sites byte-for-byte.
 */
internal object NumberFormatEs {

    private const val GROUP = ','
    private const val DECIMAL = '.'

    /** Insert comma thousands separators into a non-negative integer string of digits. */
    private fun groupDigits(digits: String): String {
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        val firstGroup = digits.length % 3
        var index = 0
        if (firstGroup > 0) {
            sb.append(digits, 0, firstGroup)
            index = firstGroup
        }
        while (index < digits.length) {
            if (sb.isNotEmpty()) sb.append(GROUP)
            sb.append(digits, index, index + 3)
            index += 3
        }
        return sb.toString()
    }

    /** Grouped integer with no fraction part — e.g. 1234567 -> "1,234,567". For [value] >= 0. */
    fun integer(value: Long): String = groupDigits(value.toString())

    /**
     * Grouped integer rounded to zero fraction digits — e.g. 1234.56 -> "1,235",
     * 1234.50 -> "1,234". Mirrors `NumberFormat.getNumberInstance(es-PE)` with min/max
     * fraction digits = 0, which uses HALF_EVEN (banker's) rounding by default. For [value] >= 0.
     */
    fun integerRounded(value: Double): String = groupDigits(roundHalfEven(abs(value)).toString())

    /** HALF_EVEN rounding to a whole number, matching `java.text.NumberFormat`'s default. */
    private fun roundHalfEven(value: Double): Long {
        val floorValue = floor(value)
        val diff = value - floorValue
        val floorLong = floorValue.toLong()
        return when {
            diff < 0.5 -> floorLong
            diff > 0.5 -> floorLong + 1
            // Exactly .5 → round to the nearest even integer.
            floorLong % 2 == 0L -> floorLong
            else -> floorLong + 1
        }
    }

    /**
     * Grouped value with exactly two fraction digits — e.g. 1234.56 -> "1,234.56",
     * 1234567.5 -> "1,234,567.50", 0.0 -> "0.00". Mirrors `DecimalFormat("#,##0.00")` (US) and
     * `NumberFormat.getNumberInstance(es-PE)` with min/max fraction digits = 2.
     */
    fun decimal2(value: Double): String {
        val totalCents = (abs(value) * 100.0).roundToLong()
        val intUnits = totalCents / 100
        val cents = totalCents % 100
        val grouped = groupDigits(intUnits.toString())
        val centsStr = cents.toString().padStart(2, '0')
        return "$grouped$DECIMAL$centsStr"
    }
}
