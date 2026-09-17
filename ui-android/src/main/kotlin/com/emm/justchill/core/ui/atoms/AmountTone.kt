package com.emm.justchill.core.ui.atoms

import androidx.compose.ui.graphics.Color
import com.emm.justchill.core.theme.EmmColors

enum class AmountTone {
    Neutral,
    Pos,
    Mute,
}

fun AmountTone.color(colors: EmmColors): Color = when (this) {
    AmountTone.Neutral -> colors.textPrimary
    AmountTone.Pos -> colors.success
    AmountTone.Mute -> colors.textTertiary
}
