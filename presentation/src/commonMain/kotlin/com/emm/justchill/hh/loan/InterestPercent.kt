package com.emm.justchill.hh.loan

private const val BPS_PER_PERCENT_POINT = 100
private const val PERCENT_DECIMALS = 2

/**
 * Out-of-range values pass through unclamped, so the domain's own range check is the one place
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
    return (combined.toLongOrNull() ?: Int.MAX_VALUE.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

internal fun bpsToPercentText(bps: Int): String {
    val whole = bps / BPS_PER_PERCENT_POINT
    val fraction = bps % BPS_PER_PERCENT_POINT
    return if (fraction == 0) whole.toString() else "$whole.${fraction.toString().padStart(PERCENT_DECIMALS, '0')}"
}

/**
 * Drops any keystroke [percentTextToBps] would silently discard, so the field never displays text
 * it does not parse. Digits pass through; the first decimal separator (period or comma) is kept,
 * every later one is dropped.
 */
internal fun sanitizeInterestPercentInput(rawText: String): String {
    val builder = StringBuilder()
    var hasSeparator = false
    for (ch in rawText) {
        when {
            ch.isDigit() -> builder.append(ch)

            (ch == '.' || ch == ',') && !hasSeparator -> {
                builder.append(ch)
                hasSeparator = true
            }
        }
    }
    return builder.toString()
}
