package com.emm.justchill.hh.report.components

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
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone

/**
 * Pill showing absolute delta and percent vs previous month.
 *
 * §7.14: leading arrow icon, text "S/ 660 · 12%", tone Pos/Neg.
 * The "vs abril" label is rendered separately beside this pill by the caller.
 *
 * [directionUp] controls the arrow icon (raw delta direction).
 * [isPositive] controls the tone — semantic favorability for the user:
 *   Income: positive when delta ≥ 0; Spend: positive when delta ≤ 0.
 */
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
                .padding(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
