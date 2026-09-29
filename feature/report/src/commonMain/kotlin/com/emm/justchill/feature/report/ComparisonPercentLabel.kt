package com.emm.justchill.feature.report

private const val COMPARISON_PERCENT_CAP = 999

internal fun comparisonPercentLabel(percent: Int): String =
    if (percent > COMPARISON_PERCENT_CAP) "más de $COMPARISON_PERCENT_CAP%" else percent.toString() + "%"
