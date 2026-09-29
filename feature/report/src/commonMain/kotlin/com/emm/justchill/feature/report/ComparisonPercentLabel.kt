package com.emm.justchill.feature.report

private const val COMPARISON_PERCENT_CAP = 999

internal fun comparisonPercentLabel(percent: Int): String =
    if (percent > COMPARISON_PERCENT_CAP) "más de $COMPARISON_PERCENT_CAP%" else percent.toString() + "%"

internal fun comparisonPillText(absoluteDeltaFormatted: String, percent: Int): String =
    "$absoluteDeltaFormatted · ${comparisonPercentLabel(percent)}"

internal fun comparisonPillDescription(absoluteDeltaFormatted: String, percent: Int, directionUp: Boolean): String {
    val verb: String = if (directionUp) "Subió" else "Bajó"
    return "$verb $absoluteDeltaFormatted, ${comparisonPercentLabel(percent)}"
}
