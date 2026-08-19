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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddCategoryScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onCategorySave: (Category) -> Unit = {},
    vm: AddCategoryViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val currentOnCategorySave by rememberUpdatedState(onCategorySave)
    val dismissAndBack = {
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
    val colors = LocalEmmColors.current
    val focusManager = LocalFocusManager.current
    val nameFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (state.name.isBlank()) nameFocus.requestFocus()
    }

    val attemptSave = {
        if (state.isAllFieldValidated) {
            focusManager.clearFocus()
            onIntent(AddCategoryIntent.OnSave)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        JcTopBar(
            title = "Nueva categoría",
            left = {
                IconBtn(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = onBack,
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val selectedIcon = AppIconCatalog.findById(state.iconId)
            val selectedColor = findById(state.colorId)

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PreviewChip(
                    name = state.name,
                    icon = selectedIcon,
                    color = selectedColor,
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
                    accent = selectedColor.primary,
                    onSelect = { onIntent(AddCategoryIntent.OnIconChange(it.id)) },
                )
            }

            Section(eyebrow = "COLOR") {
                ColorRow(
                    selected = selectedColor,
                    onSelect = { onIntent(AddCategoryIntent.OnColorChange(it.id)) },
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        StickyCTA(
            label = saveButtonLabel(state),
            tone = CtaTone.Accent,
            interaction = if (state.isAllFieldValidated) CtaInteraction.Enabled else CtaInteraction.Disabled,
            onClick = attemptSave,
        )
    }
}

private fun saveButtonLabel(state: AddCategoryUiState): String {
    val trimmed = state.name.trim()
    return if (trimmed.isBlank()) "Escribe un nombre" else "Crear «$trimmed»"
}

@Composable
private fun Section(eyebrow: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow(text = eyebrow)
        content()
    }
}

@Composable
private fun PreviewChip(
    name: String,
    icon: IconCatalog,
    color: CategoryColor,
    type: CategoryType,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current

    val displayName = name.trim().ifBlank { "Tu categoría" }
    val nameColor = if (name.isBlank()) colors.textTertiary else colors.textPrimary
    val shape = RoundedCornerShape(999.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(shape)
            .background(colors.surface1)
            .border(1.dp, colors.border, shape)
            .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface3),
        ) {
            Icon(
                imageVector = icon.icon,
                contentDescription = null,
                tint = color.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = displayName,
            fontSize = 15.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = nameColor,
            letterSpacing = (-0.15).sp,
        )
        TypeBadge(type = type)
    }
}

@Composable
private fun TypeBadge(type: CategoryType) {
    val colors = LocalEmmColors.current
    val isIncome = type == CategoryType.Income
    val bg = if (isIncome) colors.posMuted else colors.negMuted
    val fg = if (isIncome) colors.success else colors.danger
    val label = if (isIncome) "Ingreso" else "Gasto"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = fg,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun NameInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onImeAction: () -> Unit,
) {
    val colors = LocalEmmColors.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            letterSpacing = (-0.18).sp,
        ),
        cursorBrush = SolidColor(colors.accent),
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
            .padding(vertical = 8.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = "ej. Comida",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W400,
                        fontFamily = InterFontFamily,
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
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface1)
            .border(1.dp, colors.border, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(9.dp)
    val bg = if (selected) colors.surface3 else Color.Transparent
    val fg = if (selected) colors.textPrimary else colors.textSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontFamily = InterFontFamily,
            fontWeight = if (selected) FontWeight.W600 else FontWeight.W500,
            color = fg,
        )
    }
}

@Composable
private fun IconGrid(selected: IconCatalog, accent: Color, onSelect: (IconCatalog) -> Unit) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
    ) {
        items(AppIconCatalog.catalog, key = IconCatalog::id) { icon ->
            IconCell(
                icon = icon,
                selected = icon == selected,
                accent = accent,
                onClick = { onSelect(icon) },
            )
        }
    }
}

@Composable
private fun IconCell(icon: IconCatalog, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(12.dp)
    val border = if (selected) accent else colors.border
    val bg = if (selected) accent.copy(alpha = 0.14f) else colors.surface1
    val tint = if (selected) accent else colors.textSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = icon.icon,
            contentDescription = icon.name,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ColorRow(selected: CategoryColor, onSelect: (CategoryColor) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(allColors, key = CategoryColor::id) { color ->
            ColorDot(
                color = color,
                selected = color == selected,
                onClick = { onSelect(color) },
            )
        }
    }
}

@Composable
private fun ColorDot(color: CategoryColor, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalEmmColors.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.primary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Seleccionado",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
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
