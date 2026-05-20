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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors

/**
 * 48dp square icon button with 1dp border and 12dp corner radius.
 *
 * Larger than the 44dp default to be comfortable on mobile (Material recommends 48dp min).
 */
@Composable
fun IconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    tone: IconBtnTone = IconBtnTone.Neutral,
    modifier: Modifier = Modifier,
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

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
    }
}
