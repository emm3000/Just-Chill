package com.emm.justchill.feature.report

private const val COMPARISON_PERCENT_CAP = 999

internal fun comparisonPercentLabel(percent: Int): String =
    if (percent > COMPARISON_PERCENT_CAP) "más de $COMPARISON_PERCENT_CAP%" else percent.toString() + "%"

internal fun comparisonPillText(absoluteDeltaFormatted: String, percent: Int): String =
    "$absoluteDeltaFormatted · ${comparisonPercentLabel(percent)}"

internal fun comparisonPillDescription(absoluteDeltaFormatted: String, percent: Int, directionUp: Boolean?): String =
    when (directionUp) {
        true -> "Subió $absoluteDeltaFormatted, ${comparisonPercentLabel(percent)}"
        false -> "Bajó $absoluteDeltaFormatted, ${comparisonPercentLabel(percent)}"
        null -> "Sin cambio, ${comparisonPercentLabel(percent)}"
    }
