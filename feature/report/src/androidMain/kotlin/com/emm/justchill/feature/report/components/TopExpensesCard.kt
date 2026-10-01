package com.emm.justchill.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.emm.justchill.core.presentation.category.AppIconCatalog
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.category.icon
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.feature.report.TopCategoryItem

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
            .border(width = spacing.hairline, color = colors.border, shape = radii.rM)
            .padding(spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(spacing.s1),
            itemVerticalAlignment = Alignment.CenterVertically,
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
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    val icon: ImageVector = AppIconCatalog.findById(item.iconKey).icon

    TopCategoryRowLayout(
        gap = spacing.s3,
        icon = { IconTile(icon = icon, size = IconTileSize.Md) },
        labels = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s1)) {
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
        },
        amount = {
            Text(
                text = item.totalFormatted,
                style = type.amountS,
                color = colors.textPrimary,
            )
        },
    )
}

@Composable
private fun TopCategoryRowLayout(
    gap: Dp,
    icon: @Composable () -> Unit,
    labels: @Composable () -> Unit,
    amount: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(icon, labels, amount),
        modifier = Modifier.fillMaxWidth(),
    ) { measurables: List<List<Measurable>>, constraints: Constraints ->
        val gapPx: Int = gap.roundToPx()
        val center: Alignment.Vertical = Alignment.CenterVertically
        val iconPlaceable: Placeable = measurables[0].single().measure(Constraints())
        val labelsX: Int = iconPlaceable.width + gapPx
        val labelsSpace: Int = (constraints.maxWidth - labelsX).coerceAtLeast(0)
        val amountPlaceable: Placeable = measurables[2].single().measure(Constraints(maxWidth = labelsSpace))
        val labelsMeasurable: Measurable = measurables[1].single()
        val besideSpace: Int = (labelsSpace - amountPlaceable.width - gapPx).coerceAtLeast(0)
        val fitsBeside: Boolean = labelsMeasurable.minIntrinsicWidth(Constraints.Infinity) <= besideSpace
        val labelsWidth: Int = if (fitsBeside) besideSpace else labelsSpace
        val labelsPlaceable: Placeable = labelsMeasurable.measure(
            Constraints(minWidth = labelsWidth, maxWidth = labelsWidth),
        )
        val labelsBandHeight: Int = if (fitsBeside) {
            maxOf(iconPlaceable.height, labelsPlaceable.height, amountPlaceable.height)
        } else {
            maxOf(iconPlaceable.height, labelsPlaceable.height)
        }
        val amountY: Int = if (fitsBeside) 0 else labelsBandHeight + gapPx
        val amountBandHeight: Int = if (fitsBeside) labelsBandHeight else amountPlaceable.height
        layout(constraints.maxWidth, amountY + amountBandHeight) {
            iconPlaceable.placeRelative(0, center.align(iconPlaceable.height, labelsBandHeight))
            labelsPlaceable.placeRelative(labelsX, center.align(labelsPlaceable.height, labelsBandHeight))
            amountPlaceable.placeRelative(
                constraints.maxWidth - amountPlaceable.width,
                amountY + center.align(amountPlaceable.height, amountBandHeight),
            )
        }
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
                .padding(LocalEmmSpacing.current.s4),
        ) {
            TopExpensesCard(
                items = listOf(
                    TopCategoryItem(
                        categoryId = "1",
                        name = "Comida",
                        iconKey = "Fastfood",
                        totalFormatted = "S/ 1,840",
                        topMetaText = "Top en 4 de 6 meses",
                    ),
                    TopCategoryItem(
                        categoryId = "2",
                        name = "Transporte",
                        iconKey = "DirectionsBus",
                        totalFormatted = "S/ 960",
                        topMetaText = "Top en 3 de 6 meses",
                    ),
                    TopCategoryItem(
                        categoryId = "3",
                        name = "Alquiler",
                        iconKey = "Home",
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
                .padding(LocalEmmSpacing.current.s4),
        ) {
            TopCategoryRow(
                item = TopCategoryItem(
                    categoryId = "1",
                    name = "Cuidado personal y salud",
                    iconKey = "Fastfood",
                    totalFormatted = "S/ 999,999.99",
                    topMetaText = "Top en 6 de 6 meses",
                ),
            )
        }
    }
}
