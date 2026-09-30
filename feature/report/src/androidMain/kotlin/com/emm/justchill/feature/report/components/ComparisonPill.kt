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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.presentation.format.CURRENCY_PREFIX
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun ComparisonPill(
    text: String,
    description: String,
    directionUp: Boolean?,
    isPositive: Boolean?,
    modifier: Modifier = Modifier,
) {
    val tone: PillTone = comparisonPillTone(isPositive)
    val icon: ImageVector? = when (directionUp) {
        true -> Icons.Filled.ArrowUpward
        false -> Icons.Filled.ArrowDownward
        null -> null
    }

    Pill(
        text = text,
        tone = tone,
        leadingIcon = icon,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    )
}

@Preview
@Composable
private fun ComparisonPillPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val type: EmmType = LocalEmmType.current
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
                    text = "${CURRENCY_PREFIX}660 · 12%",
                    description = "Bajó ${CURRENCY_PREFIX}660, 12%",
                    directionUp = false,
                    isPositive = true,
                )
                ComparisonPill(
                    text = "${CURRENCY_PREFIX}0 · 0%",
                    description = "Sin cambio, 0%",
                    directionUp = null,
                    isPositive = null,
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
