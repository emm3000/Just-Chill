package com.emm.justchill.hh.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.category.CategoryType
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddCategoryScreen(
    onBack: () -> Unit,
    onSelectIcon: () -> Unit,
    snackbarHostState: SnackbarHostState,
    vm: AddCategoryViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AddCategoryEffect.CategorySaved -> onBack()
                is AddCategoryEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AddCategoryContent(
        state = state,
        onMoreIconsClick = onSelectIcon,
        onIntent = vm::onIntent,
        navigateToBack = onBack,
    )
}

@Composable
private fun AddCategoryContent(
    state: AddCategoryUiState,
    onMoreIconsClick: () -> Unit = {},
    onIntent: (AddCategoryIntent) -> Unit,
    navigateToBack: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding(),
    ) {

        TopBar(title = "Nueva categoría", onClose = navigateToBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s6),
        ) {

            Spacer(Modifier.height(spacing.s2))

            CategoryPreview(state)

            EmmTextInput(
                value = state.name,
                onValueChange = { onIntent(AddCategoryIntent.OnNameChange(it)) },
                label = "NOMBRE",
                placeholder = "ejm. Comida",
                modifier = Modifier.fillMaxWidth(),
            )

            TypeSection(
                selected = state.categoryType,
                onSelect = { onIntent(AddCategoryIntent.OnCategoryTypeChange(it)) },
            )

            IconSection(
                selected = state.icon,
                onSelect = { onIntent(AddCategoryIntent.OnIconChange(it)) },
                onMore = onMoreIconsClick,
            )

            ColorSection(
                selected = state.color,
                onSelect = { onIntent(AddCategoryIntent.OnColorChange(it)) },
            )

            Spacer(Modifier.height(spacing.s4))
        }

        EmmButton(
            text = "Guardar categoría",
            onClick = { onIntent(AddCategoryIntent.OnSave) },
            enabled = state.isAllFieldValidated,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun TopBar(title: String, onClose: () -> Unit) {
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
                    onClick = onClose,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Cerrar",
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

@Composable
private fun CategoryPreview(state: AddCategoryUiState) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface1, radii.rM)
            .border(1.dp, colors.border, radii.rM)
            .padding(spacing.s6),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = state.icon.icon,
                contentDescription = null,
                tint = state.color.primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(spacing.s3))
        Text(
            text = state.name.ifBlank { "Tu categoría" },
            style = type.titleL,
            color = colors.textPrimary,
        )
        Text(
            text = state.categoryType.label,
            style = type.caption,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun TypeSection(selected: CategoryType, onSelect: (CategoryType) -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Text(text = "TIPO", style = type.labelM, color = colors.textTertiary)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.s6)) {
            TypePill(label = "INGRESO", isSelected = selected == CategoryType.Income) {
                onSelect(CategoryType.Income)
            }
            TypePill(label = "GASTO", isSelected = selected == CategoryType.Spend) {
                onSelect(CategoryType.Spend)
            }
        }
    }
}

@Composable
private fun TypePill(label: String, isSelected: Boolean, onClick: () -> Unit) {
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
private fun IconSection(
    selected: IconCatalog,
    onSelect: (IconCatalog) -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Text(text = "ICONO", style = type.labelM, color = colors.textTertiary)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
            maxItemsInEachRow = 6,
        ) {
            AppIconCatalog.catalog.take(11).forEach { icon ->
                val isSelected = icon == selected
                val borderColor = if (isSelected) colors.accentFocus else colors.border
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .background(colors.surface1, radii.rS)
                        .border(if (isSelected) 1.5.dp else 1.dp, borderColor, radii.rS)
                        .clickable { onSelect(icon) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon.icon,
                        contentDescription = null,
                        tint = colors.textPrimary,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(colors.surface1, radii.rS)
                    .border(1.dp, colors.border, radii.rS)
                    .clickable { onMore() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = "Más iconos",
                    tint = colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun ColorSection(
    selected: CategoryColor,
    onSelect: (CategoryColor) -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Text(text = "COLOR", style = type.labelM, color = colors.textTertiary)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            verticalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            allColors.forEach { color ->
                val isSelected = color == selected
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.primary)
                        .then(
                            if (isSelected) Modifier.border(
                                width = 2.dp,
                                color = colors.accentFocus,
                                shape = CircleShape,
                            ) else Modifier
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(color) },
                        ),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 1000)
@Composable
private fun AddCategoryScreenPreview() {
    EmmTheme {
        AddCategoryContent(
            state = AddCategoryUiState(name = "Comida"),
            onIntent = {},
        )
    }
}
