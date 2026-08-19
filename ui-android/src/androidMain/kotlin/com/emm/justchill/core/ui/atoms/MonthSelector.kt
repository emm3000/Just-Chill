package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
fun MonthSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onLabelClick: (() -> Unit)? = null,
) {
    if (onLabelClick == null) {
        PlainMonthSelector(label = label, onPrevious = onPrevious, onNext = onNext, modifier = modifier)
    } else {
        PickerMonthSelector(
            label = label,
            onPrevious = onPrevious,
            onNext = onNext,
            onLabelClick = onLabelClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun PlainMonthSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(radii.rFull)
            .border(BorderStroke(1.dp, colors.border), radii.rFull)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(radii.rFull)
                .clickable(onClick = onPrevious),
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = "Mes anterior",
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(radii.rFull)
                .clickable(onClick = onNext),
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Mes siguiente",
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PickerMonthSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLabelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(colors.surface1)
            .border(width = 1.dp, color = colors.border, shape = shape)
            .padding(horizontal = spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        ChevronButton(
            icon = Icons.Outlined.ChevronLeft,
            contentDescription = "Mes anterior",
            onClick = onPrevious,
        )
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLabelClick,
                )
                .padding(horizontal = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = type.labelL,
                color = colors.textPrimary,
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp),
            )
        }
        ChevronButton(
            icon = Icons.Outlined.ChevronRight,
            contentDescription = "Mes siguiente",
            onClick = onNext,
        )
    }
}

@Composable
private fun ChevronButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
