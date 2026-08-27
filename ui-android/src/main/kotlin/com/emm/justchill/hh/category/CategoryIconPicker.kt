package com.emm.justchill.hh.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors

@Composable
internal fun IconGrid(selected: IconCatalog, accent: Color, onSelect: (IconCatalog) -> Unit) {
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
                accent = accent,
                onClick = { onSelect(icon) },
            )
        }
    }
}

@Composable
private fun IconCell(icon: IconCatalog, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(12.dp)
    val border = if (selected) accent else colors.border
    val bg = if (selected) accent.copy(alpha = 0.14f) else colors.surface1
    val tint = if (selected) accent else colors.textSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = icon.icon,
            contentDescription = icon.name,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
