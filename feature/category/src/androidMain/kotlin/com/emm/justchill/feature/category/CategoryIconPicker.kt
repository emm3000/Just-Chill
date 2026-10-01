package com.emm.justchill.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.emm.justchill.core.presentation.category.AppIconCatalog
import com.emm.justchill.core.presentation.category.CategoryIcon
import com.emm.justchill.core.ui.category.icon
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

private const val ICON_GRID_ROWS: Int = 2

@Composable
internal fun IconGrid(selected: CategoryIcon, onSelect: (CategoryIcon) -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val gridHeight: Dp = spacing.s12 * ICON_GRID_ROWS + spacing.s2 * (ICON_GRID_ROWS - 1)
    var query: String by rememberSaveable { mutableStateOf("") }
    val icons: List<CategoryIcon> = AppIconCatalog.search(query)

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s3)) {
        IconSearchField(query = query, onQueryChange = { query = it })
        if (icons.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
            ) {
                Text(
                    text = "Sin resultados para «$query»",
                    style = type.bodyM,
                    color = colors.textTertiary,
                )
            }
        } else {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(ICON_GRID_ROWS),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
            ) {
                items(icons, key = CategoryIcon::id) { icon: CategoryIcon ->
                    IconCell(
                        icon = icon,
                        selected = icon == selected,
                        onClick = { onSelect(icon) },
                    )
                }
            }
        }
    }
}

@Composable
private fun IconSearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val type: EmmType = LocalEmmType.current
    val shape: Shape = radii.rM

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface1)
            .border(spacing.hairline, colors.border, shape)
            .padding(spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(spacing.s4),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Buscar",
                    style = type.bodyM,
                    color = colors.textTertiary,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(colors.borderFocus),
                textStyle = type.bodyM.copy(color = colors.textPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Buscar" },
            )
        }
    }
}

@Composable
private fun IconCell(icon: CategoryIcon, selected: Boolean, onClick: () -> Unit) {
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
            contentDescription = icon.label,
            tint = tint,
            modifier = Modifier.size(spacing.s5),
        )
    }
}
