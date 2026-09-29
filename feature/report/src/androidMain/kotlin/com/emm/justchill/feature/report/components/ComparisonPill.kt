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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.feature.report.comparisonPercentLabel

@Composable
fun ComparisonPill(
    absoluteDeltaFormatted: String,
    percent: Int,
    directionUp: Boolean,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    val tone: PillTone = comparisonPillTone(isPositive)
    val icon = if (directionUp) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
    val text = "$absoluteDeltaFormatted · ${comparisonPercentLabel(percent)}"
    val description: String = comparisonPillDescription(absoluteDeltaFormatted, percent, directionUp)

    Pill(
        text = text,
        tone = tone,
        leadingIcon = icon,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    )
}

internal fun comparisonPillDescription(absoluteDeltaFormatted: String, percent: Int, directionUp: Boolean): String {
    val verb: String = if (directionUp) "Subió" else "Bajó"
    return "$verb $absoluteDeltaFormatted, ${comparisonPercentLabel(percent)}"
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
                    directionUp = false,
                    isPositive = true,
                )
                ComparisonPill(
                    absoluteDeltaFormatted = "S/ 660",
                    percent = 12,
                    directionUp = true,
                    isPositive = false,
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
