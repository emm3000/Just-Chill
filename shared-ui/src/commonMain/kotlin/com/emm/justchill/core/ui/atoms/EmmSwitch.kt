package com.emm.justchill.core.ui.atoms

import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.emm.justchill.core.theme.LocalEmmColors

/**
 * Switch colors aligned with the EmmColors design system.
 *
 * Material3's default unchecked colors map the thumb and border to `colorScheme.outline`
 * (our near-invisible hairline `border`) over a `surface3` track of almost the same gray, so
 * an OFF switch has no usable contrast on a `surface1` card. This sets explicit unchecked
 * tokens — a `textTertiary` knob and outline over a `surface3` track — so the OFF state stays
 * visible while the ON state keeps its [checkedColor] tint.
 *
 * @param checkedColor accent used for the ON state (e.g. `accent` for amount, `success` for active).
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
