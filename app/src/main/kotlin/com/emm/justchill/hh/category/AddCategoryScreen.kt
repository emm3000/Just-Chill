@file:OptIn(ExperimentalMaterial3Api::class)

package com.emm.justchill.hh.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.category.CategoryType
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.modalScreenInsets
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddCategoryScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    showSuccessMessage: (String) -> Unit = {},
    vm: AddCategoryViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val dismissAndBack = {
        keyboard?.hide()
        onBack()
    }

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AddCategoryEffect.CategorySaved -> {
                    showSuccessMessage("Categoría creada")
                    dismissAndBack()
                }
                is AddCategoryEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AddCategoryContent(
        state = state,
        onIntent = vm::onIntent,
        onBack = dismissAndBack,
    )
}

@Composable
private fun AddCategoryContent(
    state: AddCategoryUiState,
    onIntent: (AddCategoryIntent) -> Unit,
    onBack: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .modalScreenInsets(),
    ) {
        TopBar(title = "Nueva categoría", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s6),
        ) {
            Spacer(Modifier.height(spacing.s4))

            EmmTextInput(
                value = state.name,
                onValueChange = { onIntent(AddCategoryIntent.OnNameChange(it)) },
                label = "NOMBRE",
                placeholder = "ej. Comida",
                modifier = Modifier.fillMaxWidth(),
                focusRequester = focusRequester,
            )

            TypeSegmented(
                selected = state.categoryType,
                onSelect = { onIntent(AddCategoryIntent.OnCategoryTypeChange(it)) },
            )

            IconRow(
                selected = state.icon,
                onSelect = { onIntent(AddCategoryIntent.OnIconChange(it)) },
            )

            ColorRow(
                selected = state.color,
                onSelect = { onIntent(AddCategoryIntent.OnColorChange(it)) },
            )

            Spacer(Modifier.height(spacing.s4))
        }

        EmmButton(
            text = "Crear",
            onClick = { onIntent(AddCategoryIntent.OnSave) },
            enabled = state.isAllFieldValidated,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        )
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

@Composable
private fun TypeSegmented(
    selected: CategoryType,
    onSelect: (CategoryType) -> Unit,
) {
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val options = listOf(CategoryType.Income to "Ingreso", CategoryType.Spend to "Gasto")

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Text(text = "TIPO", style = type.labelM, color = LocalEmmColors.current.textTertiary)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (categoryType, label) ->
                SegmentedButton(
                    selected = selected == categoryType,
                    onClick = { onSelect(categoryType) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(text = label, style = type.labelL) },
                )
            }
        }
    }
}

@Composable
private fun IconRow(
    selected: IconCatalog,
    onSelect: (IconCatalog) -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Text(text = "ICONO", style = type.labelM, color = colors.textTertiary)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            contentPadding = PaddingValues(horizontal = spacing.s1),
        ) {
            items(AppIconCatalog.catalog, key = IconCatalog::id) { icon ->
                val isSelected = icon == selected
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.surface2 else colors.surface1)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) colors.accentFocus else colors.border,
                            shape = CircleShape,
                        )
                        .clickable { onSelect(icon) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon.icon,
                        contentDescription = icon.name,
                        tint = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorRow(
    selected: CategoryColor,
    onSelect: (CategoryColor) -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Text(text = "COLOR", style = type.labelM, color = colors.textTertiary)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            contentPadding = PaddingValues(horizontal = spacing.s1),
        ) {
            items(allColors, key = CategoryColor::id) { color ->
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

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
@Composable
private fun AddCategoryScreenPreview() {
    EmmTheme {
        AddCategoryContent(
            state = AddCategoryUiState(name = "Comida"),
            onIntent = {},
        )
    }
}
