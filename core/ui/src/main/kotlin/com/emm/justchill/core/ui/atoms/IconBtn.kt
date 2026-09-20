package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

private const val DISABLED_ALPHA = 0.35f

/**
 * Disabled dims the button without collapsing its box. `DatePickerSheet` flanks the month label
 * with two of these in a `SpaceBetween` row, so hiding one at the boundary would shift the label.
 */
@Suppress("LongParameterList")
@Composable
fun IconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    // No default: an unlabelled button is mute to TalkBack, so a decorative icon has to pass `null` and say so.
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tone: IconBtnTone = IconBtnTone.Neutral,
    enabled: Boolean = true,
    glyphSize: Dp = LocalEmmSpacing.current.s5,
) {
    val colors = LocalEmmColors.current

    val borderColor = when (tone) {
        IconBtnTone.Neutral -> colors.border
        IconBtnTone.Primary -> colors.textPrimary
        IconBtnTone.Danger -> colors.danger
    }
    val iconColor = when (tone) {
        IconBtnTone.Neutral -> colors.textSecondary
        IconBtnTone.Primary -> colors.textPrimary
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
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(glyphSize),
        )
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun IconBtnPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            IconBtn(icon = Icons.Outlined.Close, onClick = {}, contentDescription = "Cerrar")
            IconBtn(
                icon = Icons.Outlined.Delete,
                onClick = {},
                contentDescription = "Eliminar",
                tone = IconBtnTone.Danger,
            )
        }
    }
}
