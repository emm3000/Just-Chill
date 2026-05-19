package com.emm.justchill.hh.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmButtonVariant
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.modalScreenInsets
import com.emm.justchill.hh.transaction.SelectableCategory
import com.emm.justchill.hh.transaction.SignChip

@Composable
fun SelectCategoryScreen(
    onCategorySelected: (SelectableCategory) -> Unit,
    onBack: () -> Unit,
    onNewCategory: (CategoryType, String) -> Unit,
    onValueChange: (String) -> Unit,
    onTypeChange: (CategoryType) -> Unit,
    value: String,
    selectedType: CategoryType,
    activeList: List<SelectableCategory>,
    activeCountTotal: Int,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .modalScreenInsets(),
    ) {
        TopBar(title = "Selecciona categoría", onBack = onBack)

        Column(
            modifier = Modifier
                .padding(horizontal = spacing.s4)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.s4),
        ) {
            SearchInput(
                value = value,
                onValueChange = onValueChange,
                onClear = { onValueChange("") },
            )

            TypeRow(
                selectedType = selectedType,
                countTotal = activeCountTotal,
                onFlip = {
                    onTypeChange(
                        when (selectedType) {
                            CategoryType.Income -> CategoryType.Spend
                            CategoryType.Spend -> CategoryType.Income
                        }
                    )
                },
            )
        }

        Spacer(Modifier.height(spacing.s4))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (activeList.isEmpty()) {
                EmptyState(
                    query = value,
                    selectedType = selectedType,
                    onCreate = { onNewCategory(selectedType, value.trim()) },
                )
            } else {
                CategoryList(activeList, onCategorySelected)
            }
        }

        EmmButton(
            text = "+ Nueva categoría",
            onClick = { onNewCategory(selectedType, "") },
            variant = EmmButtonVariant.Secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        )
    }
}

@Composable
private fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val colors = LocalEmmColors.current
    EmmTextInput(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Buscar…",
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingContent = if (value.isNotEmpty()) {
            {
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClear,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Limpiar búsqueda",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else null,
    )
}

@Composable
private fun TypeRow(
    selectedType: CategoryType,
    countTotal: Int,
    onFlip: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val sign = if (selectedType == CategoryType.Income) "+" else "−"
    val label = if (selectedType == CategoryType.Income) "Ingresos" else "Gastos"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        SignChip(sign = sign, onClick = onFlip)
        Column {
            Text(text = label, style = type.titleL, color = colors.textPrimary)
            Text(
                text = "$countTotal categoría${if (countTotal == 1) "" else "s"}",
                style = type.caption,
                color = colors.textTertiary,
            )
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<SelectableCategory>,
    onSelect: (SelectableCategory) -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(categories, key = { it.categoryId.value }) { category ->
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelect(category) },
                    )
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
                Icon(
                    imageVector = category.icon.icon,
                    contentDescription = null,
                    tint = category.color.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = category.name,
                    style = type.bodyL,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    query: String,
    selectedType: CategoryType,
    onCreate: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val hasQuery = query.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s4, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (hasQuery) "Sin resultados para «${query.trim()}»"
            else "No tienes categorías de ${if (selectedType == CategoryType.Income) "ingreso" else "gasto"}",
            style = type.bodyL,
            color = colors.textSecondary,
        )
        if (hasQuery) {
            EmmButton(
                text = "+ Crear «${query.trim()}»",
                onClick = onCreate,
                variant = EmmButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Volver",
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = title,
            style = type.titleL,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
@Composable
private fun SelectCategoryScreenPreview() {
    EmmTheme {
        val categories = remember {
            buildList {
                repeat(6) {
                    add(
                        SelectableCategory(
                            categoryId = CategoryId("$it"),
                            name = "Categoría $it",
                            icon = AppIconCatalog.catalog[it],
                            categoryType = CategoryType.Income,
                            color = allColors[it],
                        )
                    )
                }
            }
        }
        SelectCategoryScreen(
            activeList = categories,
            activeCountTotal = categories.size,
            selectedType = CategoryType.Income,
            onBack = {},
            onValueChange = {},
            onTypeChange = {},
            onNewCategory = { _, _ -> },
            value = "",
            onCategorySelected = {},
        )
    }
}
