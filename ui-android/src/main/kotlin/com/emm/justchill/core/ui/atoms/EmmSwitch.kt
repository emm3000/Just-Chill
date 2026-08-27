package com.emm.justchill.core.ui.atoms

import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.emm.justchill.core.theme.LocalEmmColors

/**
 * Material3's own unchecked colors land on this palette's `border` over `surface3` — two grays one
 * hex step apart — so the OFF thumb and its outline disappear inside the track. The explicit
 * unchecked tokens keep them visible.
 */
@Composable
fun emmSwitchColors(checkedColor: Color): SwitchColors {
    val colors = LocalEmmColors.current
    return SwitchDefaults.colors(
        checkedThumbColor = checkedColor,
        checkedTrackColor = checkedColor.copy(alpha = 0.3f),
        uncheckedThumbColor = colors.textTertiary,
        uncheckedTrackColor = colors.surface3,
        uncheckedBorderColor = colors.textTertiary,
    )
}
