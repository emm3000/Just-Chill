package com.emm.justchill.feature.category

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.emm.justchill.core.ui.category.AppIconCatalog
import com.emm.justchill.core.ui.category.IconCatalog
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

private const val ICON_GRID_ROWS: Int = 2

@Composable
internal fun IconGrid(selected: IconCatalog, onSelect: (IconCatalog) -> Unit) {
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val gridHeight: Dp = spacing.s12 * ICON_GRID_ROWS + spacing.s2 * (ICON_GRID_ROWS - 1)

    LazyHorizontalGrid(
        rows = GridCells.Fixed(ICON_GRID_ROWS),
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
    ) {
        items(AppIconCatalog.catalog, key = IconCatalog::id) { icon: IconCatalog ->
            IconCell(
                icon = icon,
                selected = icon == selected,
                onClick = { onSelect(icon) },
            )
        }
    }
}

@Composable
private fun IconCell(icon: IconCatalog, selected: Boolean, onClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val shape: Shape = radii.rM
    val border: Color = if (selected) colors.borderFocus else colors.border
    val tint: Color = if (selected) colors.textPrimary else colors.textSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(spacing.s12)
            .clip(shape)
            .border(spacing.hairline, border, shape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon.icon,
            contentDescription = icon.name,
            tint = tint,
            modifier = Modifier.size(spacing.s5),
        )
    }
}
