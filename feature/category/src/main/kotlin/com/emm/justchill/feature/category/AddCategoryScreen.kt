package com.emm.justchill.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.ui.atoms.BackBtn
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.core.ui.category.AppIconCatalog
import com.emm.justchill.core.ui.category.IconCatalog
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddCategoryScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onCategorySave: (Category) -> Unit = {},
    vm: AddCategoryViewModel = koinViewModel(),
) {
    val state: AddCategoryUiState by vm.state.collectAsStateWithLifecycle()
    val keyboard: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
    val currentOnCategorySave: (Category) -> Unit by rememberUpdatedState(onCategorySave)
    val dismissAndBack: () -> Unit = {
        keyboard?.hide()
        onBack()
    }

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is AddCategoryEffect.CategorySaved -> {
                    keyboard?.hide()
                    currentOnCategorySave(effect.created)
                }

                is AddCategoryEffect.ShowError -> snackbarHostState.showEmmSnackbar(
                    message = effect.message,
                    tone = EmmSnackbarTone.Error,
                )
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
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val focusManager: FocusManager = LocalFocusManager.current
    val nameFocus: FocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (state.name.isBlank()) nameFocus.requestFocus()
    }

    val attemptSave: () -> Unit = {
        if (state.isAllFieldValidated) {
            focusManager.clearFocus()
            onIntent(AddCategoryIntent.OnSave)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        JcTopBar(
            title = "Nueva categoría",
            left = { BackBtn(onClick = onBack) },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s5),
        ) {
            val selectedIcon: IconCatalog = AppIconCatalog.findById(state.iconId)

            Spacer(Modifier.height(spacing.s1))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PreviewChip(
                    name = state.name,
                    icon = selectedIcon,
                    colorId = state.colorId,
                    type = state.categoryType,
                )
            }

            Section(eyebrow = "NOMBRE") {
                NameInput(
                    value = state.name,
                    onValueChange = { onIntent(AddCategoryIntent.OnNameChange(it)) },
                    focusRequester = nameFocus,
                    onImeAction = { attemptSave() },
                )
            }

            Section(eyebrow = "TIPO") {
                TypeSegmented(
                    selected = state.categoryType,
                    onSelect = { onIntent(AddCategoryIntent.OnCategoryTypeChange(it)) },
                )
            }

            Section(eyebrow = "ÍCONO") {
                IconGrid(
                    selected = selectedIcon,
                    onSelect = { onIntent(AddCategoryIntent.OnIconChange(it.id)) },
                )
            }

            Section(eyebrow = "COLOR") {
                ColorRow(
                    selected = state.colorId,
                    onSelect = { onIntent(AddCategoryIntent.OnColorChange(it)) },
                )
            }

            Spacer(Modifier.height(spacing.s2))
        }

        StickyCTA(
            label = saveButtonLabel(state),
            interaction = if (state.isAllFieldValidated) CtaInteraction.Enabled else CtaInteraction.Disabled,
            onClick = attemptSave,
        )
    }
}

private fun saveButtonLabel(state: AddCategoryUiState): String {
    val trimmed: String = state.name.trim()
    return if (trimmed.isBlank()) "Escribe un nombre" else "Crear «$trimmed»"
}

@Composable
private fun Section(eyebrow: String, content: @Composable () -> Unit) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Eyebrow(text = eyebrow)
        content()
    }
}

@Composable
private fun NameInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onImeAction: () -> Unit,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = type.titleL.copy(color = colors.textPrimary, fontWeight = FontWeight.W500),
        cursorBrush = SolidColor(colors.borderFocus),
        singleLine = true,
        keyboardActions = KeyboardActions(onDone = { onImeAction() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .drawBehind {
                drawLine(
                    color = colors.border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            }
            .padding(vertical = spacing.s2),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = "ej. Comida",
                        style = type.titleL,
                        fontWeight = FontWeight.W400,
                        color = colors.textTertiary,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun TypeSegmented(selected: CategoryType, onSelect: (CategoryType) -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val shape: Shape = radii.rM

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface1)
            .border(spacing.hairline, colors.border, shape)
            .padding(spacing.s1),
        horizontalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        TypeSegCell(
            label = "Ingreso",
            selected = selected == CategoryType.Income,
            onClick = { onSelect(CategoryType.Income) },
            modifier = Modifier.weight(1f),
        )
        TypeSegCell(
            label = "Gasto",
            selected = selected == CategoryType.Spend,
            onClick = { onSelect(CategoryType.Spend) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TypeSegCell(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val type: EmmType = LocalEmmType.current
    val shape: Shape = radii.rXS
    val bg: Color = if (selected) colors.surface3 else Color.Transparent
    val fg: Color = if (selected) colors.textPrimary else colors.textSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = spacing.s3),
    ) {
        Text(
            text = label,
            style = type.labelM,
            fontWeight = if (selected) FontWeight.W600 else FontWeight.W500,
            color = fg,
        )
    }
}

@Preview
@Composable
private fun AddCategoryScreenPreview() {
    EmmTheme {
        AddCategoryContent(
            state = AddCategoryUiState(
                name = "Regalos",
                categoryType = CategoryType.Spend,
                isAllFieldValidated = true,
            ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun AddCategoryScreenEmptyPreview() {
    EmmTheme {
        AddCategoryContent(
            state = AddCategoryUiState(),
            onIntent = {},
        )
    }
}
