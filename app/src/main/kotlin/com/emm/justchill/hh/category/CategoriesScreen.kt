package com.emm.justchill.hh.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.IconTileTone
import com.emm.justchill.core.ui.atoms.JcTopBar

@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onIntent: (CategoriesIntent) -> Unit,
    onAddCategory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        JcTopBar(
            title = "Categorías",
            left = { IconBtn(icon = Icons.AutoMirrored.Outlined.ArrowBack, onClick = onBack) },
            right = { IconBtn(icon = Icons.Outlined.Add, onClick = onAddCategory) },
        )

        if (state.categories.isEmpty()) {
            EmptyState(onCreate = onAddCategory, modifier = Modifier.fillMaxSize())
            return@Column
        }

        val incomes = state.categories
            .filter { it.categoryType == CategoryType.Income }
            .sortedBy { it.name.lowercase() }
        val spends = state.categories
            .filter { it.categoryType == CategoryType.Spend }
            .sortedBy { it.name.lowercase() }

        LazyColumn(contentPadding = PaddingValues(bottom = spacing.s8)) {
            if (incomes.isNotEmpty()) {
                item(key = "header-incomes") {
                    SectionHeader(label = "Ingresos", count = incomes.size)
                }
                items(incomes, key = { "income-${it.categoryId.value}" }) { category ->
                    CategoryRow(
                        category = category,
                        movementCount = state.txCountByCategory[category.categoryId] ?: 0,
                        onClick = { onIntent(CategoriesIntent.OnEditClick(category)) },
                    )
                }
            }
            // GASTOS always shows "Sin categoría" row at the end, so the count is +1
            item(key = "header-spends") {
                SectionHeader(label = "Gastos", count = spends.size + 1)
            }
            items(spends, key = { "spend-${it.categoryId.value}" }) { category ->
                CategoryRow(
                    category = category,
                    movementCount = state.txCountByCategory[category.categoryId] ?: 0,
                    onClick = { onIntent(CategoriesIntent.OnEditClick(category)) },
                )
            }
            item(key = "uncategorized-spend") {
                UncategorizedRow(count = state.uncategorizedSpendCount)
            }
        }
    }

    state.pendingEdit?.let { editing ->
        EditCategoryDialog(
            name = state.editName,
            onNameChange = { onIntent(CategoriesIntent.OnEditNameChange(it)) },
            onConfirm = { onIntent(CategoriesIntent.OnEditConfirm) },
            onDismiss = { onIntent(CategoriesIntent.OnEditDismiss) },
            onDelete = {
                onIntent(CategoriesIntent.OnEditDismiss)
                onIntent(CategoriesIntent.OnDeleteClick(editing))
            },
        )
    }

    state.pendingDelete?.let { category ->
        DeleteCategoryDialog(
            categoryName = category.name,
            onConfirm = { onIntent(CategoriesIntent.OnDeleteConfirm) },
            onDismiss = { onIntent(CategoriesIntent.OnDeleteDismiss) },
        )
    }
}

@Composable
private fun SectionHeader(label: String, count: Int) {
    val spacing = LocalEmmSpacing.current
    Eyebrow(
        text = "$label · $count",
        modifier = Modifier.padding(
            start = spacing.s5,
            end = spacing.s5,
            top = spacing.s5,
            bottom = spacing.s2,
        ),
    )
}

@Composable
private fun CategoryRow(
    category: Category,
    movementCount: Int,
    onClick: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val swatch = remember(category.color) { findById(category.color).primary }
    val icon = remember(category.icon) { AppIconCatalog.findById(category.icon).icon }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg: Color = if (isPressed) colors.surface1 else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = spacing.s5, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(
            icon = icon,
            size = IconTileSize.Md,
            tone = IconTileTone.Swatch,
            swatch = swatch,
        )
        Text(
            text = category.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        MovementMeta(count = movementCount, muted = false)
        Spacer(Modifier.size(6.dp))
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun UncategorizedRow(count: Int) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s5, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            size = IconTileSize.Md,
            tone = IconTileTone.Neutral,
        )
        Text(
            text = "Sin categoría",
            fontSize = 14.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textTertiary,
            modifier = Modifier.weight(1f),
        )
        MovementMeta(count = count, muted = true)
        Spacer(Modifier.size(6.dp))
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textDisabled,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun MovementMeta(count: Int, muted: Boolean) {
    val colors = LocalEmmColors.current
    Text(
        text = "$count mov.",
        fontSize = 13.sp,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W500,
        color = if (muted) colors.textDisabled else colors.textTertiary,
    )
}

@Composable
private fun EditCategoryDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalEmmColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar categoría") },
        text = {
            Column {
                EmmTextInput(
                    value = name,
                    onValueChange = onNameChange,
                    label = "NOMBRE",
                    placeholder = "ejm. Supermercado",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDelete) {
                    Text(text = "Borrar categoría", color = colors.danger)
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun DeleteCategoryDialog(
    categoryName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalEmmColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Borrar «$categoryName»?") },
        text = {
            Text(
                "Los movimientos asociados pasarán a «Sin categoría». " +
                    "Esta acción no se puede deshacer."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Borrar", color = colors.danger)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun EmptyState(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.padding(horizontal = spacing.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Category,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(spacing.s4))
        Text(
            text = "Aún sin categorías",
            style = type.headlineM,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = "Crea una para empezar a clasificar tus movimientos",
            style = type.bodyM,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(spacing.s6))
        Box(
            modifier = Modifier
                .clickable(onClick = onCreate)
                .background(colors.accent)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Crear categoría",
                fontSize = 13.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.W600,
                color = Color.White,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun CategoriesScreenPreview() {
    EmmTheme {
        CategoriesScreen(
            state = CategoriesUiState(
                categories = listOf(
                    Category(CategoryId("1"), "Sueldo", "wallet", "green", CategoryType.Income),
                    Category(CategoryId("2"), "Freelance", "bolt", "yellow", CategoryType.Income),
                    Category(CategoryId("3"), "Ventas IG", "store", "purple", CategoryType.Income),
                    Category(CategoryId("4"), "Yapes", "wallet", "blue", CategoryType.Income),
                    Category(CategoryId("5"), "Comida", "restaurant", "red", CategoryType.Spend),
                    Category(CategoryId("6"), "Transporte", "directions_car", "orange", CategoryType.Spend),
                    Category(CategoryId("7"), "Servicios", "bolt", "yellow", CategoryType.Spend),
                    Category(CategoryId("8"), "Ocio", "movie", "purple", CategoryType.Spend),
                    Category(CategoryId("9"), "Salud", "favorite", "green", CategoryType.Spend),
                ),
                txCountByCategory = mapOf(
                    CategoryId("1") to 1, CategoryId("2") to 4, CategoryId("3") to 7, CategoryId("4") to 3,
                    CategoryId("5") to 8, CategoryId("6") to 5, CategoryId("7") to 2,
                    CategoryId("8") to 4, CategoryId("9") to 1,
                ),
                uncategorizedSpendCount = 0,
            ),
            onIntent = {},
            onAddCategory = {},
            onBack = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun CategoriesScreenEmptyPreview() {
    EmmTheme {
        CategoriesScreen(
            state = CategoriesUiState(),
            onIntent = {},
            onAddCategory = {},
            onBack = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
