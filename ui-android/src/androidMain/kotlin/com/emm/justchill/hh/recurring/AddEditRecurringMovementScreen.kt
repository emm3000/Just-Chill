package com.emm.justchill.hh.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.emmSwitchColors
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.shared.AmountInputSheet
import com.emm.justchill.hh.shared.FormSection
import com.emm.justchill.hh.transaction.centsToSoles
import com.emm.justchill.hh.transaction.resolvedColor
import com.emm.justchill.hh.transaction.sheets.AccountPickerSheet
import com.emm.justchill.hh.transaction.sheets.CategoryPickerSheet
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddEditRecurringMovementScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    id: String? = null,
    vm: AddEditRecurringMovementViewModel = koinViewModel(
        parameters = { parametersOf(id) },
    ),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AddEditRecurringMovementEffect.NavigateBack -> currentOnBack()

                is AddEditRecurringMovementEffect.ShowError -> snackbarHostState.showEmmSnackbar(
                    message = effect.message,
                    tone = EmmSnackbarTone.Error,
                )
            }
        }
    }

    AddEditRecurringMovementContent(
        state = state,
        onIntent = vm::onIntent,
        onBack = currentOnBack,
    )
}

@Composable
private fun AddEditRecurringMovementContent(
    state: AddEditRecurringMovementUiState,
    onIntent: (AddEditRecurringMovementIntent) -> Unit,
    onBack: () -> Unit = {},
) {
    val colors = LocalEmmColors.current

    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDaySheet by remember { mutableStateOf(false) }
    var showAmountSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        val title = if (state.isEdit) "Editar recurrente" else "Nuevo recurrente"
        JcTopBar(
            title = title,
            left = {
                IconBtn(
                    icon = Icons.Outlined.Close,
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
            Spacer(Modifier.height(4.dp))

            FormSection(eyebrow = "NOMBRE") {
                NameInput(
                    value = state.name,
                    onValueChange = { onIntent(AddEditRecurringMovementIntent.OnNameChange(it)) },
                )
            }

            FormSection(eyebrow = "TIPO") {
                TypeToggle(
                    selected = state.type,
                    onSelect = { onIntent(AddEditRecurringMovementIntent.OnTypeChange(it)) },
                )
            }

            FormSection(eyebrow = "MONTO") {
                AmountCardSection(
                    amountDigits = state.amountDigits,
                    isVariable = state.isVariableAmount,
                    type = state.type,
                    onVariableToggle = { onIntent(AddEditRecurringMovementIntent.OnVariableAmountToggle(it)) },
                    onOpenSheet = { showAmountSheet = true },
                )
            }

            SelectorPillsRow(
                state = state,
                onOpenAccount = { showAccountPicker = true },
                onOpenCategory = { showCategoryPicker = true },
                onOpenDay = { showDaySheet = true },
            )

            ActiveCard(
                isActive = state.isActive,
                onToggle = { onIntent(AddEditRecurringMovementIntent.OnIsActiveChange(it)) },
            )

            FormSection(eyebrow = "DESCRIPCIÓN · OPCIONAL") {
                NameInput(
                    value = state.description,
                    placeholder = "Ej. Pago mensual streaming",
                    onValueChange = { onIntent(AddEditRecurringMovementIntent.OnDescriptionChange(it)) },
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        val ctaLabel = if (state.isEdit) "Guardar cambios" else "Crear recurrente"
        StickyCTA(
            label = ctaLabel,
            tone = CtaTone.Accent,
            interaction = if (state.isSaveEnabled) CtaInteraction.Enabled else CtaInteraction.Disabled,
            onClick = { onIntent(AddEditRecurringMovementIntent.Save) },
        )
    }

    if (showAccountPicker) {
        AccountPickerSheet(
            accounts = state.accounts,
            selectedAccountId = state.selectedAccount?.accountId?.value,
            onSelect = { account ->
                onIntent(AddEditRecurringMovementIntent.OnAccountSelected(account))
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            categories = state.categories,
            selectedCategoryId = state.selectedCategory?.categoryId?.value,
            onSelect = { category ->
                onIntent(AddEditRecurringMovementIntent.OnCategorySelected(category))
                showCategoryPicker = false
            },
            onAddNew = { showCategoryPicker = false },
            onDismiss = { showCategoryPicker = false },
        )
    }

    if (showDaySheet) {
        DayOfMonthSheet(
            current = state.dayOfMonth,
            onConfirm = { day ->
                onIntent(AddEditRecurringMovementIntent.OnDayOfMonthChange(day))
                showDaySheet = false
            },
            onDismiss = { showDaySheet = false },
        )
    }

    if (showAmountSheet) {
        val typeLabel = if (state.type == TransactionType.Income) "Ingreso" else "Gasto"
        AmountInputSheet(
            amountDigits = state.amountDigits,
            title = "Monto del recurrente",
            tone = if (state.type == TransactionType.Income) AmountTone.Pos else AmountTone.Neg,
            onAmountConfirm = { onIntent(AddEditRecurringMovementIntent.OnAmountChange(it)) },
            onDismiss = { showAmountSheet = false },
            subtitle = "$typeLabel · se paga cada mes",
        )
    }
}

@Composable
private fun NameInput(value: String, onValueChange: (String) -> Unit, placeholder: String = "Ej. Netflix") {
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
        modifier = Modifier
            .fillMaxWidth()
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
                        text = placeholder,
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

private val TYPE_OPTIONS = listOf(
    TransactionType.Income,
    TransactionType.Spend,
)

private fun typeGlyph(type: TransactionType): String = when (type) {
    TransactionType.Income -> "+"
    TransactionType.Spend -> "−"
}

@Composable
private fun TypeToggle(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    val colors = LocalEmmColors.current

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TYPE_OPTIONS.forEach { type ->
            val isSelected = selected == type
            val shape = RoundedCornerShape(12.dp)
            val borderColor = if (isSelected) colors.textPrimary else colors.border
            val bgColor = if (isSelected) colors.surface3 else colors.surface1
            val textColor = if (isSelected) colors.textPrimary else colors.textSecondary

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(shape)
                    .background(bgColor)
                    .border(1.dp, borderColor, shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(type) },
                    ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = typeGlyph(type),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600,
                        fontFamily = InterFontFamily,
                        color = textColor,
                    )
                    Text(
                        text = type.label,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W500,
                        fontFamily = InterFontFamily,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountCardSection(
    amountDigits: String,
    isVariable: Boolean,
    type: TransactionType,
    onVariableToggle: (Boolean) -> Unit,
    onOpenSheet: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val tone = if (type == TransactionType.Income) AmountTone.Pos else AmountTone.Neg

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val cardInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(radii.rM)
                .background(colors.surface1)
                .border(1.dp, colors.border, radii.rM)
                .then(
                    if (!isVariable) {
                        Modifier.clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = onOpenSheet,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isVariable || amountDigits.isEmpty()) {
                Text(
                    text = "S/ —.—",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = InterFontFamily,
                    color = colors.textTertiary,
                    letterSpacing = (-0.8).sp,
                )
            } else {
                AmountHero(
                    value = centsToSoles(amountDigits),
                    tone = tone,
                    showCaret = false,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(radii.rM)
                .background(colors.surface1)
                .border(1.dp, colors.border, radii.rM)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monto variable",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = InterFontFamily,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Lo defines al confirmar cada mes.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W400,
                        fontFamily = InterFontFamily,
                        color = colors.textTertiary,
                    )
                }
                Switch(
                    checked = isVariable,
                    onCheckedChange = onVariableToggle,
                    colors = emmSwitchColors(checkedColor = colors.accent),
                )
            }
        }
    }
}

@Composable
private fun SelectorPill(
    eyebrow: String,
    value: String,
    onClick: () -> Unit,
    dotColor: Color? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(radii.rM)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Eyebrow(text = eyebrow)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (dotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                }
                if (trailingIcon != null) {
                    trailingIcon()
                }
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                    letterSpacing = (-0.13).sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SelectorPillsRow(
    state: AddEditRecurringMovementUiState,
    onOpenAccount: () -> Unit,
    onOpenCategory: () -> Unit,
    onOpenDay: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val categoryDotColor = state.selectedCategory?.resolvedColor?.primary ?: colors.accent

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            SelectorPill(
                eyebrow = "CUENTA",
                value = state.selectedAccount?.name ?: "Seleccionar",
                dotColor = colors.accent,
                onClick = onOpenAccount,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            SelectorPill(
                eyebrow = "CATEGORÍA",
                value = state.selectedCategory?.name ?: "Sin categoría",
                dotColor = categoryDotColor,
                onClick = onOpenCategory,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            SelectorPill(
                eyebrow = "DÍA",
                value = "Día ${state.dayOfMonth}",
                onClick = onOpenDay,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(11.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun ActiveCard(isActive: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(radii.rM)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rM)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Activo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Mientras esté pausado no genera pendientes en tu Inicio.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    fontFamily = InterFontFamily,
                    color = colors.textTertiary,
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = emmSwitchColors(checkedColor = colors.success),
            )
        }
    }
}

@Preview
@Composable
private fun AddEditRecurringMovementScreenPreview() {
    EmmTheme {
        AddEditRecurringMovementContent(
            state = AddEditRecurringMovementUiState(
                name = "Netflix",
                type = TransactionType.Spend,
                dayOfMonth = 15,
                isActive = true,
            ),
            onIntent = {},
        )
    }
}
