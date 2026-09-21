package com.emm.justchill.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun ComparisonPill(
    absoluteDeltaFormatted: String,
    percent: Int,
    directionUp: Boolean,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    val tone = if (isPositive) PillTone.Pos else PillTone.Neg
    val icon = if (directionUp) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
    val text = "$absoluteDeltaFormatted · $percent%"

    Pill(
        text = text,
        tone = tone,
        leadingIcon = icon,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ComparisonPillPreview() {
    EmmTheme {
        val colors = LocalEmmColors.current
        val type = LocalEmmType.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(LocalEmmSpacing.current.s4),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LocalEmmSpacing.current.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ComparisonPill(
                    absoluteDeltaFormatted = "S/ 660",
                    percent = 12,
                    directionUp = true,
                    isPositive = true,
                )
                Text(
                    text = "vs. Abril",
                    style = type.bodyM,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
