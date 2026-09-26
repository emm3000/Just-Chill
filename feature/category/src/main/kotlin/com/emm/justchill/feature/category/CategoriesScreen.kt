package com.emm.justchill.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.ui.atoms.BackBtn
import com.emm.justchill.core.ui.atoms.CategoryDot
import com.emm.justchill.core.ui.atoms.ChevronTrailing
import com.emm.justchill.core.ui.atoms.DialogAction
import com.emm.justchill.core.ui.atoms.EmmDialog
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.FilledCta
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.IconBtnTone
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.category.AppIconCatalog
import com.emm.justchill.core.ui.category.resolvedColor
import com.emm.justchill.core.ui.components.EmmTextInput
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onIntent: (CategoriesIntent) -> Unit,
    onAddCategory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        JcTopBar(
            title = "Categorías",
            left = { BackBtn(onClick = onBack) },
            right = {
                IconBtn(
                    icon = Icons.Outlined.Add,
                    onClick = onAddCategory,
                    contentDescription = "Nueva categoría",
                )
            },
            column = spacing.s5,
        )

        if (state.categories.isEmpty()) {
            EmptyState(onCreate = onAddCategory, modifier = Modifier.fillMaxSize())
            return@Column
        }

        val incomes: List<Category> = state.categories
            .filter { it.categoryType == CategoryType.Income }
            .sortedBy { it.name.lowercase() }
        val spends: List<Category> = state.categories
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
            affectedCount = state.txCountByCategory[category.categoryId] ?: 0,
            onConfirm = { onIntent(CategoriesIntent.OnDeleteConfirm) },
            onDismiss = { onIntent(CategoriesIntent.OnDeleteDismiss) },
        )
    }
}

@Composable
private fun SectionHeader(label: String, count: Int) {
    val spacing: EmmSpacing = LocalEmmSpacing.current
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
private fun CategoryRow(category: Category, movementCount: Int, onClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val icon: ImageVector = remember(category.icon) { AppIconCatalog.findById(category.icon).icon }

    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
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
            .padding(horizontal = spacing.s5, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        IconTile(icon = icon, size = IconTileSize.Md)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier.weight(1f),
        ) {
            CategoryDot(color = colors.resolvedColor(category.color))
            Text(
                text = category.name,
                style = type.titleM,
                fontWeight = FontWeight.W500,
                color = colors.textPrimary,
            )
        }
        MovementMeta(count = movementCount, muted = false)
        Spacer(Modifier.size(spacing.s1))
        ChevronTrailing()
    }
}

@Composable
private fun UncategorizedRow(count: Int) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s5, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        IconTile(icon = Icons.AutoMirrored.Outlined.HelpOutline, size = IconTileSize.Md)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier.weight(1f),
        ) {
            CategoryDot(color = colors.catGraphite)
            Text(
                text = "Sin categoría",
                style = type.titleM,
                fontWeight = FontWeight.W500,
                color = colors.textTertiary,
            )
        }
        MovementMeta(count = count, muted = true)
        Spacer(Modifier.size(spacing.s1))
        ChevronTrailing(enabled = false)
    }
}

@Composable
private fun MovementMeta(count: Int, muted: Boolean) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    Text(
        text = "$count mov.",
        style = type.labelM,
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
    val spacing: EmmSpacing = LocalEmmSpacing.current

    EmmDialog(
        title = "Editar categoría",
        confirmLabel = "Guardar",
        onConfirm = onConfirm,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        destructiveAction = {
            DialogAction(label = "Borrar categoría", onClick = onDelete, tone = IconBtnTone.Danger)
        },
        content = {
            EmmTextInput(
                value = name,
                onValueChange = onNameChange,
                label = "NOMBRE",
                placeholder = "ejm. Supermercado",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(spacing.s3))
        },
    )
}

@Composable
private fun DeleteCategoryDialog(
    categoryName: String,
    affectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current

    EmmDialog(
        title = "¿Borrar «$categoryName»?",
        confirmLabel = "Borrar",
        onConfirm = onConfirm,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        confirmTone = IconBtnTone.Danger,
    ) {
        Text(text = buildDeleteCategoryMessage(affectedCount), style = type.bodyM, color = colors.textSecondary)
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.padding(horizontal = spacing.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Category,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(spacing.s10),
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
        FilledCta(
            label = "Crear categoría",
            onClick = onCreate,
        )
    }
}

@Preview
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

@Preview
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

@Preview
@PreviewRedmi15CWidth
@Composable
private fun CategoryRowOverflowPreview() {
    EmmTheme {
        CategoryRow(
            category = Category(
                CategoryId("1"),
                "Cuidado personal y salud",
                "wallet",
                "green",
                CategoryType.Spend,
            ),
            movementCount = 999,
            onClick = {},
        )
    }
}
