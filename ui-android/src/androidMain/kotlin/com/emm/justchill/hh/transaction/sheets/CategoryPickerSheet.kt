package com.emm.justchill.hh.transaction.sheets

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.IconTileTone
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.hh.transaction.SelectableCategory
import com.emm.justchill.hh.transaction.resolvedColor
import com.emm.justchill.hh.transaction.resolvedIcon

private const val LIST_MAX_HEIGHT_FRACTION = 0.55f

@Composable
fun CategoryPickerSheet(
    categories: List<SelectableCategory>,
    selectedCategoryId: String?,
    onSelect: (SelectableCategory) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit,
    frequentCategoryIds: List<String> = emptyList(),
) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
    val maxListHeight = screenHeightDp * LIST_MAX_HEIGHT_FRACTION

    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(categories, query) {
        if (query.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }
    }
    val sections = remember(categories, frequentCategoryIds, query) {
        if (query.isNotBlank()) {
            null
        } else {
            val frequent = frequentCategoryIds.mapNotNull { id ->
                categories.firstOrNull { it.categoryId.value == id }
            }
            if (frequent.size < 2) {
                null
            } else {
                val frequentIdSet = frequentCategoryIds.toSet()
                val rest = categories.filter { it.categoryId.value !in frequentIdSet }
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
                .padding(start = 24.dp, end = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Selecciona categoría",
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

        val searchShape = RoundedCornerShape(12.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 14.dp)
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
                        text = "Buscar o crear",
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

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Sin categorías. Crea una primero.",
                    fontSize = 13.sp,
                    color = colors.textTertiary,
                    fontFamily = InterFontFamily,
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
                        val isActive = category.categoryId.value == selectedCategoryId
                        CategoryRow(
                            category = category,
                            isActive = isActive,
                            accentColor = colors.accent,
                            textPrimary = colors.textPrimary,
                            activeBg = colors.surface1,
                            onClick = {
                                onSelect(category)
                                onDismiss()
                            },
                        )
                    }
                    item("header_all") { SectionHeader("Todas") }
                    items(rest, key = { "rest_" + it.categoryId.value }) { category ->
                        val isActive = category.categoryId.value == selectedCategoryId
                        CategoryRow(
                            category = category,
                            isActive = isActive,
                            accentColor = colors.accent,
                            textPrimary = colors.textPrimary,
                            activeBg = colors.surface1,
                            onClick = {
                                onSelect(category)
                                onDismiss()
                            },
                        )
                    }
                } else {
                    items(filtered, key = { it.categoryId.value }) { category ->
                        val isActive = category.categoryId.value == selectedCategoryId
                        CategoryRow(
                            category = category,
                            isActive = isActive,
                            accentColor = colors.accent,
                            textPrimary = colors.textPrimary,
                            activeBg = colors.surface1,
                            onClick = {
                                onSelect(category)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }

        val addButtonShape = RoundedCornerShape(12.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
                .height(46.dp)
                .clip(addButtonShape)
                .border(1.dp, colors.borderFocus, addButtonShape)
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
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Nueva categoría",
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Eyebrow(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 6.dp),
    )
}

@Composable
private fun CategoryRow(
    category: SelectableCategory,
    isActive: Boolean,
    accentColor: Color,
    textPrimary: Color,
    activeBg: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) activeBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(
            icon = category.resolvedIcon,
            size = IconTileSize.Sm,
            tone = IconTileTone.Swatch,
            swatch = category.resolvedColor.primary,
        )

        Text(
            text = category.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            color = textPrimary,
            letterSpacing = (-0.15).sp,
            modifier = Modifier.weight(1f),
        )

        if (isActive) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}
