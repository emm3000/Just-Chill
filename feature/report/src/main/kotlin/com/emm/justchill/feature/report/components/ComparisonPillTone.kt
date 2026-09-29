package com.emm.justchill.feature.report.components

import com.emm.justchill.core.ui.atoms.PillTone

fun comparisonPillTone(isPositive: Boolean): PillTone = if (isPositive) PillTone.Pos else PillTone.Neutral
