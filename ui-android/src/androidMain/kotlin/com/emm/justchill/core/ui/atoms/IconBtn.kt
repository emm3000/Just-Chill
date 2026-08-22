package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors

private const val DISABLED_ALPHA = 0.35f

/**
 * Disabled dims the button without collapsing its box. `DatePickerSheet` flanks the month label
 * with two of these in a `SpaceBetween` row, so hiding one at the boundary would shift the label.
 *
 * [contentDescription] has no default: an unlabelled button is mute to TalkBack, so a caller whose
 * icon is genuinely decorative has to pass `null` and say so.
 */
// icon/onClick/contentDescription are the required params; modifier, tone and enabled each cover an
// independent axis (layout, color, interactivity) — a config object would relocate them, not remove
// any.
@Suppress("LongParameterList")
@Composable
fun IconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tone: IconBtnTone = IconBtnTone.Neutral,
    enabled: Boolean = true,
) {
    val colors = LocalEmmColors.current

    val borderColor = when (tone) {
        IconBtnTone.Neutral -> colors.border
        IconBtnTone.Accent -> colors.accent
        IconBtnTone.Danger -> colors.danger
    }
    val iconColor = when (tone) {
        IconBtnTone.Neutral -> colors.textSecondary
        IconBtnTone.Accent -> colors.accent
        IconBtnTone.Danger -> colors.danger
    }
    val shape = RoundedCornerShape(12.dp)
    val alpha = if (enabled) 1f else DISABLED_ALPHA

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .alpha(alpha)
            .clip(shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
    }
}
