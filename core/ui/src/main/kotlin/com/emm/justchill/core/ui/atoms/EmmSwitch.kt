package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

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

@Preview
@PreviewRedmi15CWidth
@Composable
private fun EmmSwitchPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            Switch(checked = true, onCheckedChange = {}, colors = emmSwitchColors(colors.success))
            Switch(checked = false, onCheckedChange = {}, colors = emmSwitchColors(colors.success))
        }
    }
}
