package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

private val ChipChevronSize: Dp = 16.dp

/**
 * [onClickLabel] is what TalkBack reads as the action: the chip's own text is the current value —
 * "BCP", "Supermercado" — and says nothing about what tapping it does.
 */
@Composable
fun SelectorChip(
    label: String,
    dotColor: Color?,
    onClickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector = Icons.Outlined.KeyboardArrowDown,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(spacing.s12)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = onClickLabel,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.s10)
                .clip(radii.rFull)
                .border(1.dp, colors.border, radii.rFull)
                .padding(horizontal = spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            if (dotColor != null) {
                CategoryDot(color = dotColor)
            }
            Text(
                text = label,
                style = type.labelL,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(ChipChevronSize),
            )
        }
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun SelectorChipPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            SelectorChip(
                label = "BCP",
                dotColor = null,
                onClickLabel = "Cambiar cuenta",
                onClick = {},
            )
            SelectorChip(
                label = "Supermercado",
                dotColor = colors.catSage,
                onClickLabel = "Cambiar categoría",
                onClick = {},
            )
        }
    }
}
