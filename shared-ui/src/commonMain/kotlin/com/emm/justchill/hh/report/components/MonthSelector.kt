package com.emm.justchill.hh.report.components

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
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Month picker pill with chevrons and a centered label.
 *
 * Self-contained pill (`surface1` bg, hairline border, 44dp height). The
 * caller composes a "Hoy" shortcut as a sibling pill when needed — this
 * atom no longer renders the jump-to-today affordance internally, to
 * mirror the designer's handoff layout (selector + "Hoy" side-by-side).
 */
@Composable
fun MonthSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onLabelClick: () -> Unit = {},
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

/**
 * "Hoy" pill, shown beside [MonthSelector] when the selected month is not
 * the current one. Matches the selector's height + border style.
 */
@Composable
fun TodayPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val shape = RoundedCornerShape(999.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(colors.surface1)
            .border(width = 1.dp, color = colors.border, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = spacing.s4),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Hoy",
            style = type.labelL,
            color = colors.textPrimary,
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

@Preview
@Composable
private fun MonthSelectorPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthSelector(label = "Mayo 2026", onPrevious = {}, onNext = {})
            }
        }
    }
}

@Preview
@Composable
private fun MonthSelectorWithTodayPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthSelector(label = "Marzo 2026", onPrevious = {}, onNext = {})
                TodayPill(onClick = {})
            }
        }
    }
}
