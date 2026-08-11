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

/** Opacity of a disabled button — enough to read the icon, not enough to invite a tap. */
private const val DISABLED_ALPHA = 0.35f

/**
 * 48dp square icon button with 1dp border and 12dp corner radius.
 *
 * Larger than the 44dp default to be comfortable on mobile (Material recommends 48dp min).
 *
 * [enabled] dims the button and drops the click. It stays laid out at full size when disabled:
 * the date picker's month chevrons use it, and a control that vanishes at the boundary would
 * shift the month label sideways every time the user reached the current month.
 */
@Composable
fun IconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
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
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
    }
}
