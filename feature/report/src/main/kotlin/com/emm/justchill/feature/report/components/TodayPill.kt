package com.emm.justchill.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.atoms.MonthSelector
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun TodayPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val shape: RoundedCornerShape = LocalEmmRadii.current.rFull
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(spacing.s12)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(spacing.s12)
                .clip(shape)
                .background(colors.surface1)
                .border(width = spacing.hairline, color = colors.border, shape = shape)
                .indication(interactionSource, ripple())
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
}

@Preview
@Composable
private fun MonthSelectorWithTodayPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .background(LocalEmmColors.current.bg)
                .padding(LocalEmmSpacing.current.s4),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(LocalEmmSpacing.current.s2)) {
                MonthSelector(label = "Marzo 2026", onPrevious = {}, onNext = {}, onLabelClick = {})
                TodayPill(onClick = {})
            }
        }
    }
}
