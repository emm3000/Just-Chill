package com.emm.justchill.hh.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.hh.report.CategoryShare

/**
 * §7.10 grouped-variant card: wraps IncomeByCategoryBars in a §7.4 card with
 * header (Eyebrow + category count), bars, hairline, and stats footer.
 *
 * Footer: "X movimientos" left, "Promedio S/ Y" right (caption textSecondary).
 * Singular form: "1 movimiento".
 */
@Composable
fun CategoryBarsCard(
    shares: List<CategoryShare>,
    movementCount: Int,
    averageFormatted: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current
    val type = LocalEmmType.current

    val categoryCountText = "${shares.size} ${if (shares.size == 1) "categoría" else "categorías"}"
    val movementText = "$movementCount ${if (movementCount == 1) "movimiento" else "movimientos"}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(radii.rM)
            .background(colors.surface1)
            .border(width = 1.dp, color = colors.border, shape = radii.rM)
            .padding(spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(text = "POR CATEGORÍA")
            Text(
                text = categoryCountText,
                style = type.bodyM,
                color = colors.textSecondary,
            )
        }

        IncomeByCategoryBars(shares = shares)

        Hairline()

        // Footer row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = movementText,
                style = type.caption,
                color = colors.textSecondary,
            )
            Text(
                text = "Promedio $averageFormatted",
                style = type.caption,
                color = colors.textSecondary,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CategoryBarsCardPreview() {
    EmmTheme {
        val colors = LocalEmmColors.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(16.dp),
        ) {
            CategoryBarsCard(
                shares = listOf(
                    CategoryShare("1", "Sueldo", "S/ 4,500", 60, colors.catSlate),
                    CategoryShare("2", "Freelance", "S/ 1,200", 19, colors.catSage),
                    CategoryShare("3", "Ventas", "S/ 400", 6, colors.catTerracotta),
                ),
                movementCount = 5,
                averageFormatted = "S/ 1,233",
            )
        }
    }
}
