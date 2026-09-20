package com.emm.justchill.feature.category

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.category.AppIconCatalog
import com.emm.justchill.core.ui.category.IconCatalog
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.LocalEmmColors

@Composable
internal fun IconGrid(selected: IconCatalog, onSelect: (IconCatalog) -> Unit) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
    ) {
        items(AppIconCatalog.catalog, key = IconCatalog::id) { icon ->
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
    val shape: Shape = RoundedCornerShape(12.dp)
    val border: Color = if (selected) colors.borderFocus else colors.border
    val tint: Color = if (selected) colors.textPrimary else colors.textSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(shape)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon.icon,
            contentDescription = icon.name,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
