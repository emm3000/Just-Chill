package com.emm.justchill.hh.shared

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

object NumberFormatEs {

    // Reversed from conventional Spanish (dot-thousands, comma-decimal) to match es-PE; the golden test pins it.
    private const val GROUP = ','
    private const val DECIMAL = '.'

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

    /** Grouped integer, no fraction part. [value] must be >= 0. */
    fun integer(value: Long): String = groupDigits(value.toString())

    /** Rounds like `NumberFormat.getNumberInstance(es-PE)` at 0 fraction digits: HALF_EVEN (banker's), not HALF_UP. */
    fun integerRounded(value: Double): String = groupDigits(roundHalfEven(abs(value)).toString())

    private fun roundHalfEven(value: Double): Long {
        val floorValue = floor(value)
        val diff = value - floorValue
        val floorLong = floorValue.toLong()
        return when {
            diff < 0.5 -> floorLong
            diff > 0.5 -> floorLong + 1
            floorLong % 2 == 0L -> floorLong
            else -> floorLong + 1
        }
    }

    /** Two fraction digits from an exact cents amount; callers needing a sign prefix add it themselves. */
    fun cents(cents: Long): String {
        val abs = abs(cents)
        val grouped = groupDigits((abs / 100).toString())
        val centsStr = (abs % 100).toString().padStart(2, '0')
        return "$grouped$DECIMAL$centsStr"
    }

    /** Rounds HALF_UP to the nearest cent before delegating to [cents]. */
    fun decimal2(value: Double): String = cents((abs(value) * 100.0).roundToLong())
}
