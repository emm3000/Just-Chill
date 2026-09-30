package com.emm.justchill.feature.report.components

import com.emm.justchill.core.ui.atoms.PillTone

internal fun comparisonPillTone(isPositive: Boolean?): PillTone = when (isPositive) {
    true -> PillTone.Pos
    else -> PillTone.Neutral
}
