package com.emm.justchill.hh.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Month picker with chevrons and a centered label.
 *
 * Compartido entre Home y Report (PLAN_S1_REPORT.md §1 D3).
 * Pattern: Mint, Apple Health — chevrons + label.
 */
@Composable
fun MonthSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChevronButton(
            icon = Icons.Outlined.ChevronLeft,
            contentDescription = "Mes anterior",
            onClick = onPrevious,
        )
        Text(
            text = label,
            style = type.titleL,
            color = colors.textPrimary,
        )
        ChevronButton(
            icon = Icons.Outlined.ChevronRight,
            contentDescription = "Mes siguiente",
            onClick = onNext,
        )
    }
}

@Composable
private fun ChevronButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(48.dp)
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
            modifier = Modifier.size(24.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun MonthSelectorPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg),
        ) {
            MonthSelector(
                label = "Mayo 2026",
                onPrevious = {},
                onNext = {},
            )
        }
    }
}
