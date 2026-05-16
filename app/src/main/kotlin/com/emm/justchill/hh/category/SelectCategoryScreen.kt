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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.category.CategoryType
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmButtonVariant
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.transaction.SelectableCategory
import kotlinx.coroutines.launch

@Composable
fun SelectCategoryScreen(
    onCategorySelected: (SelectableCategory) -> Unit,
    onBack: () -> Unit,
    onNewCategory: () -> Unit = {},
    onValueChange: (String) -> Unit = {},
    value: String,
    income: List<SelectableCategory>,
    expense: List<SelectableCategory>,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding(),
    ) {

        TopBar(title = "Selecciona categoría", onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = spacing.s4)) {
            EmmTextInput(
                value = value,
                onValueChange = onValueChange,
                placeholder = "Buscar categorías...",
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(spacing.s4))

            TabHeader(
                currentPage = pagerState.currentPage,
                onSelectIncome = { scope.launch { pagerState.animateScrollToPage(0) } },
                onSelectExpense = { scope.launch { pagerState.animateScrollToPage(1) } },
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) { page ->
            val list = if (page == 0) income else expense
            CategoryList(list, onCategorySelected)
        }

        EmmButton(
            text = "Crear nueva categoría",
            onClick = onNewCategory,
            variant = EmmButtonVariant.Secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun TabHeader(
    currentPage: Int,
    onSelectIncome: () -> Unit,
    onSelectExpense: () -> Unit,
) {
    val spacing = LocalEmmSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s6),
    ) {
        TabOption(label = "INGRESO", isSelected = currentPage == 0, onClick = onSelectIncome)
        TabOption(label = "GASTO", isSelected = currentPage == 1, onClick = onSelectExpense)
    }
}

@Composable
private fun TabOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val interactionSource = remember { MutableInteractionSource() }

    val underlineColor = if (isSelected) colors.accentFocus else colors.border
    val labelColor = if (isSelected) colors.textPrimary else colors.textTertiary

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = spacing.s2)
            .drawBehind {
                val stroke = if (isSelected) 2f else 1f
                drawLine(
                    color = underlineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = stroke,
                )
            }
            .padding(bottom = spacing.s2),
    ) {
        Text(text = label, style = type.labelL, color = labelColor)
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
        items(categories, key = SelectableCategory::categoryId) { category ->
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
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Atrás",
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
                            categoryId = "$it",
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
            income = categories,
            expense = categories,
            onBack = {},
            onValueChange = {},
            value = "",
            onCategorySelected = {},
        )
    }
}
