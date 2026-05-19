package com.emm.justchill.hh.category

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmButtonVariant
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onIntent: (CategoriesIntent) -> Unit,
    onAddCategory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(modifier = modifier.background(colors.bg).statusBarsPadding()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.s2,
                    end = spacing.s2,
                    top = spacing.s6,
                    bottom = spacing.s4,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = "Categorías",
                style = type.headlineL,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAddCategory) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Nueva categoría",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

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
                item(key = "header-incomes") { SectionHeader(text = "INGRESOS") }
                items(incomes, key = { "income-${it.categoryId.value}" }) { category ->
                    CategoryRow(
                        category = category,
                        onEdit = { onIntent(CategoriesIntent.OnEditClick(category)) },
                        onDelete = { onIntent(CategoriesIntent.OnDeleteClick(category)) },
                    )
                }
            }
            if (spends.isNotEmpty()) {
                item(key = "header-spends") { SectionHeader(text = "GASTOS") }
                items(spends, key = { "spend-${it.categoryId.value}" }) { category ->
                    CategoryRow(
                        category = category,
                        onEdit = { onIntent(CategoriesIntent.OnEditClick(category)) },
                        onDelete = { onIntent(CategoriesIntent.OnDeleteClick(category)) },
                    )
                }
            }
        }
    }

    state.pendingEdit?.let {
        EditCategoryDialog(
            name = state.editName,
            onNameChange = { onIntent(CategoriesIntent.OnEditNameChange(it)) },
            onConfirm = { onIntent(CategoriesIntent.OnEditConfirm) },
            onDismiss = { onIntent(CategoriesIntent.OnEditDismiss) },
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
private fun SectionHeader(text: String) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Text(
        text = text,
        style = type.labelM,
        color = colors.textTertiary,
        modifier = Modifier.padding(
            horizontal = spacing.s4,
            vertical = spacing.s2,
        ),
    )
}

@Composable
private fun CategoryRow(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4, vertical = spacing.s4)
            .drawBehind {
                drawLine(
                    color = colors.border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Text(
            text = category.name,
            style = type.bodyL,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        CategoryRowMenu(onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
private fun CategoryRowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Opciones de categoría",
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surface2,
        ) {
            DropdownMenuItem(
                text = { Text("Editar", style = type.bodyL, color = colors.textPrimary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("Borrar", style = type.bodyL, color = colors.danger) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun EditCategoryDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar categoría") },
        text = {
            EmmTextInput(
                value = name,
                onValueChange = onNameChange,
                label = "NOMBRE",
                placeholder = "ejm. Supermercado",
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
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
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun EmptyState(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.padding(horizontal = spacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Category,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
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
        EmmButton(
            text = "Crear categoría",
            onClick = onCreate,
            variant = EmmButtonVariant.Secondary,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun CategoriesScreenPreview() {
    EmmTheme {
        CategoriesScreen(
            state = CategoriesUiState(
                categories = listOf(
                    Category(
                        categoryId = CategoryId("1"),
                        name = "Sueldo",
                        icon = "salary",
                        color = "#00FF00",
                        categoryType = CategoryType.Income,
                    ),
                    Category(
                        categoryId = CategoryId("2"),
                        name = "Freelance",
                        icon = "freelance",
                        color = "#00FF00",
                        categoryType = CategoryType.Income,
                    ),
                    Category(
                        categoryId = CategoryId("3"),
                        name = "Supermercado",
                        icon = "shopping",
                        color = "#FF0000",
                        categoryType = CategoryType.Spend,
                    ),
                    Category(
                        categoryId = CategoryId("4"),
                        name = "Restaurantes",
                        icon = "restaurant",
                        color = "#FF0000",
                        categoryType = CategoryType.Spend,
                    ),
                ),
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
