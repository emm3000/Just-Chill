package com.emm.justchill.hh.loan

private const val BPS_PER_PERCENT_POINT = 100
private const val PERCENT_DECIMALS = 2

/**
 * Truncates a percent decimal beyond [PERCENT_DECIMALS] places rather than rounding — the field
 * mirrors what the user typed. Blank text means 0%, the common interest-free loan, never an error;
 * out-of-range values pass through unclamped, so the domain's own range check is the one place
 * that rejects them.
 */
internal fun percentTextToBps(rawText: String): Int {
    val normalized = rawText.replace(',', '.')
    val digitsAndDot = normalized.filter { it.isDigit() || it == '.' }
    val integerPart = digitsAndDot.substringBefore('.')
    val fractionPart = if ('.' in digitsAndDot) {
        digitsAndDot.substringAfter('.').filter(Char::isDigit)
    } else {
        ""
    }
    val twoDecimals = fractionPart.take(PERCENT_DECIMALS).padEnd(PERCENT_DECIMALS, '0')
    val combined = integerPart.ifEmpty { "0" } + twoDecimals
    return combined.toLongOrNull()?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0
}

internal fun bpsToPercentText(bps: Int): String {
    val whole = bps / BPS_PER_PERCENT_POINT
    val fraction = bps % BPS_PER_PERCENT_POINT
    return if (fraction == 0) whole.toString() else "$whole.${fraction.toString().padStart(PERCENT_DECIMALS, '0')}"
}
