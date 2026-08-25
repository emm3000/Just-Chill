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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.IconTileTone
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.report.TopCategoryItem
import com.emm.justchill.hh.report.domainColorToUi

@Composable
fun TopExpensesCard(items: List<TopCategoryItem>, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(radii.rM)
            .background(colors.surface1)
            .border(width = 1.dp, color = colors.border, shape = radii.rM)
            .padding(spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(text = "TUS MAYORES GASTOS")
            Text(
                text = "Promedio mensual",
                style = type.bodyM,
                color = colors.textSecondary,
            )
        }

        items.forEachIndexed { index, item ->
            if (index > 0) Hairline()
            TopCategoryRow(item = item)
        }
    }
}

@Composable
private fun TopCategoryRow(item: TopCategoryItem) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val icon = AppIconCatalog.findById(item.iconKey).icon

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(
            icon = icon,
            size = IconTileSize.Md,
            tone = IconTileTone.Swatch,
            swatch = domainColorToUi(item.colorKey),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.name,
                style = type.bodyM.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.W600),
                color = colors.textPrimary,
            )
            Text(
                text = item.topMetaText,
                style = type.bodyM,
                color = colors.textSecondary,
            )
        }

        Text(
            text = item.totalFormatted,
            style = type.amountS,
            color = colors.textPrimary,
        )
    }
}

@Preview
@Composable
private fun TopExpensesCardPreview() {
    EmmTheme {
        val colors = LocalEmmColors.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(16.dp),
        ) {
            TopExpensesCard(
                items = listOf(
                    TopCategoryItem(
                        categoryId = "1",
                        name = "Comida",
                        iconKey = "Fastfood",
                        colorKey = "red",
                        totalFormatted = "S/ 1,840",
                        topMetaText = "Top en 4 de 6 meses",
                    ),
                    TopCategoryItem(
                        categoryId = "2",
                        name = "Transporte",
                        iconKey = "DirectionsBus",
                        colorKey = "blue",
                        totalFormatted = "S/ 960",
                        topMetaText = "Top en 3 de 6 meses",
                    ),
                    TopCategoryItem(
                        categoryId = "3",
                        name = "Alquiler",
                        iconKey = "Home",
                        colorKey = "purple",
                        totalFormatted = "S/ 900",
                        topMetaText = "Top en 6 de 6 meses",
                    ),
                ),
            )
        }
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun TopCategoryRowOverflowPreview() {
    EmmTheme {
        val colors = LocalEmmColors.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(16.dp),
        ) {
            TopCategoryRow(
                item = TopCategoryItem(
                    categoryId = "1",
                    name = "Cuidado personal y salud",
                    iconKey = "Fastfood",
                    colorKey = "red",
                    totalFormatted = "S/ 999,999.99",
                    topMetaText = "Top en 6 de 6 meses",
                ),
            )
        }
    }
}
