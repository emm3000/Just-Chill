package com.emm.justchill.hh.profile

import androidx.compose.ui.graphics.Color
import com.emm.justchill.core.theme.EmmColors

internal data class DialogActionStyle(val bg: Color, val border: Color, val textColor: Color)

internal fun EmmColors.neutralDialogAction() = DialogActionStyle(
    bg = surface1,
    border = border,
    textColor = textPrimary,
)

internal fun EmmColors.destructiveDialogAction() = DialogActionStyle(
    bg = danger,
    border = danger,
    textColor = textOnAccent,
)
