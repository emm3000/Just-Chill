package com.emm.justchill.core.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.emm.justchill.core.ui.atoms.CategoryDot
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.category.resolvedColor
import com.emm.justchill.core.ui.category.resolvedIcon
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

private const val LIST_MAX_HEIGHT_FRACTION: Float = 0.55f

@Composable
fun CategoryPickerSheet(
    categories: List<SelectableCategory>,
    selectedCategoryId: String?,
    onSelect: (SelectableCategory) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit,
    frequentCategoryIds: List<String> = emptyList(),
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val type: EmmType = LocalEmmType.current
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val windowInfo: WindowInfo = LocalWindowInfo.current
    val density: Density = LocalDensity.current
    val screenHeightDp: Dp = with(density) { windowInfo.containerSize.height.toDp() }
    val maxListHeight: Dp = screenHeightDp * LIST_MAX_HEIGHT_FRACTION

    var query: String by rememberSaveable { mutableStateOf("") }
    val filtered: List<SelectableCategory> = remember(categories, query) {
        if (query.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }
    }
    val sections: Pair<List<SelectableCategory>, List<SelectableCategory>>? =
        remember(categories, frequentCategoryIds, query) {
            if (query.isNotBlank()) {
                null
            } else {
                val frequent: List<SelectableCategory> = frequentCategoryIds.mapNotNull { id ->
                    categories.firstOrNull { it.categoryId.value == id }
                }
                if (frequent.size < 2) {
                    null
                } else {
                    val frequentIdSet: Set<String> = frequentCategoryIds.toSet()
                    val rest: List<SelectableCategory> = categories.filter { it.categoryId.value !in frequentIdSet }
                    Pair(frequent, rest)
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.s6, end = spacing.s2, bottom = spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Selecciona categoría",
                style = type.titleM,
                color = colors.textPrimary,
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(spacing.s12)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(spacing.s8)
                        .clip(CircleShape)
                        .background(colors.surface1)
                        .border(spacing.hairline, colors.border, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(spacing.s3),
                    )
                }
            }
        }

        val searchShape: RoundedCornerShape = radii.rM
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s5)
                .padding(bottom = spacing.s4)
                .clip(searchShape)
                .background(colors.surface1)
                .border(spacing.hairline, colors.border, searchShape)
                .padding(horizontal = spacing.s3, vertical = spacing.s3),
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
                        text = "Buscar o crear",
                        style = type.bodyM,
                        color = colors.textTertiary,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    cursorBrush = SolidColor(colors.borderFocus),
                    textStyle = type.bodyM.copy(color = colors.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.s6),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Sin categorías. Crea una primero.",
                    style = type.bodyM,
                    color = colors.textTertiary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxListHeight),
            ) {
                if (sections != null) {
                    val (frequent, rest) = sections
                    item("header_frequent") { SectionHeader("Frecuentes") }
                    items(frequent, key = { "freq_" + it.categoryId.value }) { category ->
                        val isActive: Boolean = category.categoryId.value == selectedCategoryId
                        CategoryRow(
                            category = category,
                            isActive = isActive,
                            onClick = {
                                onSelect(category)
                                onDismiss()
                            },
                        )
                    }
                    item("header_all") { SectionHeader("Todas") }
                    items(rest, key = { "rest_" + it.categoryId.value }) { category ->
                        val isActive: Boolean = category.categoryId.value == selectedCategoryId
                        CategoryRow(
                            category = category,
                            isActive = isActive,
                            onClick = {
                                onSelect(category)
                                onDismiss()
                            },
                        )
                    }
                } else {
                    items(filtered, key = { it.categoryId.value }) { category ->
                        val isActive: Boolean = category.categoryId.value == selectedCategoryId
                        CategoryRow(
                            category = category,
                            isActive = isActive,
                            onClick = {
                                onSelect(category)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }

        val addButtonShape: RoundedCornerShape = radii.rM
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.s4, end = spacing.s4, top = spacing.s3, bottom = spacing.s4)
                .height(spacing.s12)
                .clip(addButtonShape)
                .border(spacing.hairline, colors.borderFocus, addButtonShape)
                .clickable {
                    onAddNew()
                    onDismiss()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(spacing.s3),
            )
            Spacer(Modifier.size(spacing.s2))
            Text(
                text = "Nueva categoría",
                style = type.labelL.copy(fontWeight = FontWeight.W600),
                color = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val spacing: EmmSpacing = LocalEmmSpacing.current
    Eyebrow(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.s6, end = spacing.s6, top = spacing.s3, bottom = spacing.s2),
    )
}

@Composable
private fun CategoryRow(category: SelectableCategory, isActive: Boolean, onClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) colors.surface1 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.s6, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        IconTile(icon = category.resolvedIcon, size = IconTileSize.Sm)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier.weight(1f),
        ) {
            CategoryDot(color = colors.resolvedColor(category.colorId))
            Text(
                text = category.name,
                style = type.titleM.copy(fontWeight = FontWeight.W500),
                color = colors.textPrimary,
            )
        }

        if (isActive) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(spacing.s6)
                    .clip(CircleShape)
                    .background(colors.surface3),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(spacing.s3),
                )
            }
        }
    }
}
