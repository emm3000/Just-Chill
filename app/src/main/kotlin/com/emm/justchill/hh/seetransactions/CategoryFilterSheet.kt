package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.category.CategoryType
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.IconTileTone
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.findById
import java.text.Normalizer

@OptIn(ExperimentalMaterial3Api::class)
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
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var segment by rememberSaveable { mutableStateOf(initialSegment) }
    var query by rememberSaveable { mutableStateOf("") }

    val totalCount = items.size

    val filtered = remember(items, segment, query) {
        val normalizedQuery = query.normalizeForSearch()
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Filtrar por categoría",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                    letterSpacing = (-0.15).sp,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surface1)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(onClick = onDismiss),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }

            // Search
            val searchShape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
                    .clip(searchShape)
                    .background(colors.surface1)
                    .border(1.dp, colors.border, searchShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Buscar entre $totalCount categorías",
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily,
                            color = colors.textTertiary,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        cursorBrush = SolidColor(colors.accentFocus),
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily,
                            color = colors.textPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Segmented control
            SegmentedRow(
                segment = segment,
                onSegmentChange = { segment = it },
                incomeCount = incomeCount,
                spendCount = spendCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            )

            Spacer(Modifier.height(6.dp))

            // List
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

            // Footer: Limpiar filtro
            if (hasActiveFilter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .clickable {
                            onClear()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Limpiar filtro",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W600,
                        fontFamily = InterFontFamily,
                        color = colors.textPrimary,
                    )
                }
            } else {
                Spacer(Modifier.height(16.dp))
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
    val colors = LocalEmmColors.current
    val outerShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .clip(outerShape)
            .background(colors.surface1)
            .border(1.dp, colors.border, outerShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(9.dp)
    val bg = if (selected) colors.surface3 else Color.Transparent
    val textColor = if (selected) colors.textPrimary else colors.textSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
    ) {
        Text(
            text = "$label · $count",
            fontSize = 12.sp,
            fontFamily = InterFontFamily,
            fontWeight = if (selected) FontWeight.W600 else FontWeight.W500,
            color = textColor,
        )
    }
}

@Composable
private fun SheetCategoryRow(item: CategorySheetItem, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val icon = remember(item.iconId) { AppIconCatalog.findById(item.iconId) }
    val color = remember(item.colorId) { findById(item.colorId) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.isActive) colors.surface1 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(
            icon = icon.icon,
            size = IconTileSize.Sm,
            tone = IconTileTone.Swatch,
            swatch = color.primary,
        )

        Text(
            text = item.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )

        if (item.isActive) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

private fun String.normalizeForSearch(): String = Normalizer.normalize(this.trim().lowercase(), Normalizer.Form.NFD)
    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
