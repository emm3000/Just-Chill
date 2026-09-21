package com.emm.justchill.feature.transaction.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
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
import androidx.compose.ui.text.font.FontWeight
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.category.AppIconCatalog
import com.emm.justchill.core.ui.category.IconCatalog
import com.emm.justchill.core.ui.format.stripSpanishAccents
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
internal fun CategoryFilterSheet(
    items: List<CategorySheetItem>,
    incomeCount: Int,
    spendCount: Int,
    hasActiveFilter: Boolean,
    initialSegment: CategoryType,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val type: EmmType = LocalEmmType.current
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var segment by rememberSaveable { mutableStateOf(initialSegment) }
    var query by rememberSaveable { mutableStateOf("") }

    val totalCount: Int = items.size

    val filtered: List<CategorySheetItem> = remember(items, segment, query) {
        val normalizedQuery: String = query.normalizeForSearch()
        items
            .asSequence()
            .filter { it.type == segment }
            .filter { normalizedQuery.isBlank() || it.name.normalizeForSearch().contains(normalizedQuery) }
            .toList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.s6, end = spacing.s4, bottom = spacing.s4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Filtrar por categoría",
                    style = type.titleM,
                    color = colors.textPrimary,
                )
                val closeInteraction: MutableInteractionSource = remember { MutableInteractionSource() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(spacing.s12)
                        .clickable(
                            interactionSource = closeInteraction,
                            indication = null,
                            onClick = onDismiss,
                        ),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(spacing.s8)
                            .clip(CircleShape)
                            .background(colors.surface1)
                            .border(spacing.hairline, colors.border, CircleShape)
                            .indication(closeInteraction, ripple()),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s5)
                    .padding(bottom = spacing.s3)
                    .clip(radii.rM)
                    .background(colors.surface1)
                    .border(spacing.hairline, colors.border, radii.rM)
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
                            text = "Buscar entre $totalCount categorías",
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

            SegmentedRow(
                segment = segment,
                onSegmentChange = { segment = it },
                incomeCount = incomeCount,
                spendCount = spendCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s5, vertical = spacing.s1),
            )

            Spacer(Modifier.height(spacing.s2))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(filtered, key = { it.id }) { item ->
                    SheetCategoryRow(
                        item = item,
                        onClick = {
                            onSelect(item.id)
                            onDismiss()
                        },
                    )
                }
            }

            if (hasActiveFilter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = spacing.s4, end = spacing.s4, top = spacing.s3, bottom = spacing.s4)
                        .heightIn(min = spacing.s12)
                        .clip(radii.rM)
                        .border(spacing.hairline, colors.border, radii.rM)
                        .clickable {
                            onClear()
                            onDismiss()
                        }
                        .padding(horizontal = spacing.s4, vertical = spacing.s3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(spacing.s3),
                    )
                    Spacer(Modifier.width(spacing.s2))
                    Text(
                        text = "Limpiar filtro",
                        style = type.labelL,
                        fontWeight = FontWeight.W600,
                        color = colors.textPrimary,
                    )
                }
            } else {
                Spacer(Modifier.height(spacing.s4))
            }
        }
    }
}

@Composable
private fun SegmentedRow(
    segment: CategoryType,
    onSegmentChange: (CategoryType) -> Unit,
    incomeCount: Int,
    spendCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current

    Row(
        modifier = modifier
            .height(spacing.s12)
            .clip(radii.rM)
            .background(colors.surface1)
            .border(spacing.hairline, colors.border, radii.rM)
            .padding(horizontal = spacing.s1),
        horizontalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        SegmentCell(
            label = "Ingresos",
            count = incomeCount,
            selected = segment == CategoryType.Income,
            onClick = { onSegmentChange(CategoryType.Income) },
            modifier = Modifier.weight(1f),
        )
        SegmentCell(
            label = "Gastos",
            count = spendCount,
            selected = segment == CategoryType.Spend,
            onClick = { onSegmentChange(CategoryType.Spend) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentCell(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val type: EmmType = LocalEmmType.current
    val bg: Color = if (selected) colors.surface3 else Color.Transparent
    val textColor: Color = if (selected) colors.textPrimary else colors.textSecondary
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = spacing.s1),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(radii.rXS)
                .background(bg)
                .indication(interactionSource, ripple()),
        ) {
            Text(
                text = "$label · $count",
                style = type.labelM,
                fontWeight = if (selected) FontWeight.W600 else FontWeight.W500,
                color = textColor,
            )
        }
    }
}

@Composable
private fun SheetCategoryRow(item: CategorySheetItem, onClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val icon: IconCatalog = remember(item.iconId) { AppIconCatalog.findById(item.iconId) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.isActive) colors.surface1 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.s6, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        IconTile(icon = icon.icon, size = IconTileSize.Sm)

        Text(
            text = item.name,
            style = type.titleM,
            fontWeight = FontWeight.W500,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )

        if (item.isActive) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(spacing.s6)
                    .clip(CircleShape)
                    .background(colors.surface3),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(spacing.s3),
                )
            }
        }
    }
}

private fun String.normalizeForSearch(): String = this.trim().lowercase().stripSpanishAccents()
