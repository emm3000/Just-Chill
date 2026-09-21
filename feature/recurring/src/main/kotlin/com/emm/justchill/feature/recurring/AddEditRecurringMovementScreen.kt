package com.emm.justchill.feature.recurring

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.BackBtn
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.FormSection
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.core.ui.atoms.emmSwitchColors
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.core.ui.format.centsToSoles
import com.emm.justchill.core.ui.sheets.AccountPickerSheet
import com.emm.justchill.core.ui.sheets.AmountInputSheet
import com.emm.justchill.core.ui.sheets.CategoryPickerSheet
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddEditRecurringMovementScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onAddNewCategory: (CategoryType) -> Unit,
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
        onAddNewCategory = onAddNewCategory,
    )
}

@Composable
private fun AddEditRecurringMovementContent(
    state: AddEditRecurringMovementUiState,
    onIntent: (AddEditRecurringMovementIntent) -> Unit,
    onBack: () -> Unit = {},
    onAddNewCategory: (CategoryType) -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        val title = if (state.isEdit) "Editar recurrente" else "Nuevo recurrente"
        JcTopBar(
            title = title,
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
            Spacer(Modifier.height(spacing.s1))

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
                    onOpenSheet = { onIntent(AddEditRecurringMovementIntent.OnSheetRequested(RecurringSheet.Amount)) },
                )
            }

            SelectorChipsRow(
                state = state,
                onOpenAccount = { onIntent(AddEditRecurringMovementIntent.OnSheetRequested(RecurringSheet.Account)) },
                onOpenCategory = {
                    onIntent(AddEditRecurringMovementIntent.OnSheetRequested(RecurringSheet.Category))
                },
                onOpenDay = { onIntent(AddEditRecurringMovementIntent.OnSheetRequested(RecurringSheet.Day)) },
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

            Spacer(Modifier.height(spacing.s2))
        }

        val ctaLabel = if (state.isEdit) "Guardar cambios" else "Crear recurrente"
        StickyCTA(
            label = ctaLabel,
            interaction = if (state.isSaveEnabled) CtaInteraction.Enabled else CtaInteraction.Disabled,
            onClick = { onIntent(AddEditRecurringMovementIntent.Save) },
        )
    }

    if (state.openSheet == RecurringSheet.Account) {
        AccountPickerSheet(
            accounts = state.accounts,
            selectedAccountId = state.selectedAccount?.accountId?.value,
            onSelect = { account -> onIntent(AddEditRecurringMovementIntent.OnAccountSelected(account)) },
            onDismiss = { onIntent(AddEditRecurringMovementIntent.OnSheetDismissed) },
        )
    }

    if (state.openSheet == RecurringSheet.Category) {
        CategoryPickerSheet(
            categories = state.categories,
            selectedCategoryId = state.selectedCategory?.categoryId?.value,
            onSelect = { category -> onIntent(AddEditRecurringMovementIntent.OnCategorySelected(category)) },
            onAddNew = { onAddNewCategory(state.type.categoryType) },
            onDismiss = { onIntent(AddEditRecurringMovementIntent.OnSheetDismissed) },
        )
    }

    if (state.openSheet == RecurringSheet.Day) {
        DayOfMonthSheet(
            current = state.dayOfMonth,
            onConfirm = { day -> onIntent(AddEditRecurringMovementIntent.OnDayOfMonthChange(day)) },
            onDismiss = { onIntent(AddEditRecurringMovementIntent.OnSheetDismissed) },
        )
    }

    if (state.openSheet == RecurringSheet.Amount) {
        val typeLabel = if (state.type == TransactionType.Income) "Ingreso" else "Gasto"
        AmountInputSheet(
            amountDigits = state.amountDigits,
            title = "Monto del recurrente",
            tone = if (state.type == TransactionType.Income) AmountTone.Pos else AmountTone.Neutral,
            onAmountConfirm = { onIntent(AddEditRecurringMovementIntent.OnAmountChange(it)) },
            onDismiss = { onIntent(AddEditRecurringMovementIntent.OnSheetDismissed) },
            subtitle = "$typeLabel · se paga cada mes",
        )
    }
}

@Composable
private fun NameInput(value: String, onValueChange: (String) -> Unit, placeholder: String = "Ej. Netflix") {
    val colors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val typeTokens: EmmType = LocalEmmType.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = typeTokens.titleL.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.borderFocus),
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
            .padding(vertical = spacing.s2),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = typeTokens.titleL,
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
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val typeTokens: EmmType = LocalEmmType.current

    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
        TYPE_OPTIONS.forEach { type ->
            val isSelected = selected == type
            val shape = radii.rM
            val borderColor: Color = if (isSelected) colors.borderFocus else colors.border
            val bgColor = if (isSelected) colors.surface3 else colors.surface1
            val textColor = if (isSelected) colors.textPrimary else colors.textSecondary

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(spacing.s12)
                    .clip(shape)
                    .background(bgColor)
                    .border(spacing.hairline, borderColor, shape)
                    .clickable(onClick = { onSelect(type) }),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = typeGlyph(type),
                        style = typeTokens.titleM,
                        color = textColor,
                    )
                    Text(
                        text = type.label,
                        style = typeTokens.titleM.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
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
    val radii: EmmRadii = LocalEmmRadii.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val typeTokens: EmmType = LocalEmmType.current
    val tone = if (type == TransactionType.Income) AmountTone.Pos else AmountTone.Neutral

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s3)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(radii.rM)
                .background(colors.surface1)
                .border(spacing.hairline, colors.border, radii.rM)
                .then(
                    if (!isVariable) {
                        Modifier.clickable(onClick = onOpenSheet)
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = spacing.s5),
            contentAlignment = Alignment.Center,
        ) {
            if (isVariable || amountDigits.isEmpty()) {
                Text(
                    text = "S/ —.—",
                    style = typeTokens.amountCard,
                    color = colors.textTertiary,
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
                .border(spacing.hairline, colors.border, radii.rM)
                .padding(horizontal = spacing.s4, vertical = spacing.s3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monto variable",
                        style = typeTokens.labelL,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Lo defines al confirmar cada mes.",
                        style = typeTokens.labelM,
                        color = colors.textTertiary,
                    )
                }
                Switch(
                    checked = isVariable,
                    onCheckedChange = onVariableToggle,
                    colors = emmSwitchColors(checkedColor = colors.textPrimary),
                )
            }
        }
    }
}

@Composable
private fun ActiveCard(isActive: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalEmmColors.current
    val radii: EmmRadii = LocalEmmRadii.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val typeTokens: EmmType = LocalEmmType.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(radii.rM)
            .background(colors.surface1)
            .border(spacing.hairline, colors.border, radii.rM)
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Activo",
                    style = typeTokens.labelL,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Mientras esté pausado no genera pendientes.",
                    style = typeTokens.labelM,
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
