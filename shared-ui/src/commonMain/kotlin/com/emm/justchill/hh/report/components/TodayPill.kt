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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.MonthSelector

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
                MonthSelector(label = "Marzo 2026", onPrevious = {}, onNext = {}, onLabelClick = {})
                TodayPill(onClick = {})
            }
        }
    }
}
