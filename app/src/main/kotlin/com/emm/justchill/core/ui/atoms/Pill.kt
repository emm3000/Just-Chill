package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii

/**
 * Small status/tag chip.
 *
 * Padding: 2dp vertical, 8dp horizontal. Height is driven by content (11sp text).
 * Radius: [EmmRadii.rFull].
 */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, tone: PillTone = PillTone.Neutral, leadingIcon: ImageVector? = null) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    val pillColors = when (tone) {
        PillTone.Neutral -> colors.surface2 to colors.textSecondary
        PillTone.Pos -> colors.posMuted to colors.success
        PillTone.Neg -> colors.negMuted to colors.danger
        PillTone.Accent -> colors.accentMuted to colors.accent
    }
    val (bgColor, fgColor) = pillColors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(radii.rFull)
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(10.dp),
            )
        }
        Text(
            text = text,
            color = fgColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
        )
    }
}
