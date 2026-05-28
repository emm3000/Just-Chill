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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA
import com.emm.justchill.hh.transaction.sheets.AccountPickerSheet
import com.emm.justchill.hh.transaction.sheets.CategoryPickerSheet
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val MIN_DAY_OF_MONTH = 1
private const val MAX_DAY_OF_MONTH = 31

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
                is AddEditRecurringMovementEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AddEditRecurringMovementContent(
        state = state,
        onIntent = vm::onIntent,
        onBack = onBack,
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
    var showDayPicker by remember { mutableStateOf(false) }

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

            FormSection(eyebrow = "CUENTA") {
                PickerRow(
                    label = state.selectedAccount?.name ?: "Seleccionar cuenta",
                    onClick = { showAccountPicker = true },
                )
            }

            FormSection(eyebrow = "CATEGORÍA (opcional)") {
                PickerRow(
                    label = state.selectedCategory?.name ?: "Sin categoría",
                    onClick = { showCategoryPicker = true },
                )
            }

            FormSection(eyebrow = "MONTO") {
                AmountSection(
                    amountDigits = state.amountDigits,
                    isVariable = state.isVariableAmount,
                    onAmountChange = { onIntent(AddEditRecurringMovementIntent.OnAmountChange(it)) },
                    onVariableToggle = { onIntent(AddEditRecurringMovementIntent.OnVariableAmountToggle(it)) },
                )
            }

            FormSection(eyebrow = "DÍA DEL MES (1-31)") {
                PickerRow(
                    label = "Día ${state.dayOfMonth}",
                    onClick = { showDayPicker = true },
                )
            }

            FormSection(eyebrow = "ACTIVO") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (state.isActive) "Sí" else "No",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = InterFontFamily,
                        color = colors.textPrimary,
                    )
                    Switch(
                        checked = state.isActive,
                        onCheckedChange = { onIntent(AddEditRecurringMovementIntent.OnIsActiveChange(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.accent,
                            checkedTrackColor = colors.accent.copy(alpha = 0.3f),
                        ),
                    )
                }
            }

            FormSection(eyebrow = "DESCRIPCIÓN (opcional)") {
                NameInput(
                    value = state.description,
                    placeholder = "ejm. Pago mensual streaming",
                    onValueChange = { onIntent(AddEditRecurringMovementIntent.OnDescriptionChange(it)) },
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        val ctaLabel = if (state.isEdit) "Guardar cambios" else "Crear recurrente"
        StickyCTA(
            label = ctaLabel,
            tone = CtaTone.Accent,
            enabled = state.isSaveEnabled,
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

    if (showDayPicker) {
        DayOfMonthPickerDialog(
            current = state.dayOfMonth,
            onConfirm = { day ->
                onIntent(AddEditRecurringMovementIntent.OnDayOfMonthChange(day))
                showDayPicker = false
            },
            onDismiss = { showDayPicker = false },
        )
    }
}

@Composable
private fun FormSection(eyebrow: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow(text = eyebrow)
        content()
    }
}

@Composable
private fun NameInput(value: String, onValueChange: (String) -> Unit, placeholder: String = "ejm. Netflix") {
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
    TransactionType.Income to "Ingreso",
    TransactionType.Spend to "Gasto",
)

@Composable
private fun TypeToggle(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    val colors = LocalEmmColors.current

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TYPE_OPTIONS.forEach { (type, label) ->
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
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W500,
                    fontFamily = InterFontFamily,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun PickerRow(label: String, onClick: () -> Unit) {
    val colors = LocalEmmColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
            letterSpacing = (-0.15).sp,
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun AmountSection(
    amountDigits: String,
    isVariable: Boolean,
    onAmountChange: (String) -> Unit,
    onVariableToggle: (Boolean) -> Unit,
) {
    val colors = LocalEmmColors.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Monto variable",
                fontSize = 14.sp,
                fontWeight = FontWeight.W400,
                fontFamily = InterFontFamily,
                color = colors.textSecondary,
            )
            Switch(
                checked = isVariable,
                onCheckedChange = onVariableToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.accent,
                    checkedTrackColor = colors.accent.copy(alpha = 0.3f),
                ),
            )
        }

        if (!isVariable) {
            BasicTextField(
                value = amountDigits,
                onValueChange = { raw ->
                    val filtered = raw.filter(Char::isDigit).take(13)
                    onAmountChange(filtered)
                },
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = InterFontFamily,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        if (amountDigits.isEmpty()) {
                            Text(
                                text = "0 centavos",
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
    }
}

@Composable
private fun DayOfMonthPickerDialog(current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf(current.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Día del mes") },
        text = {
            BasicTextField(
                value = input,
                onValueChange = { raw ->
                    val filtered = raw.filter(Char::isDigit).take(2)
                    input = filtered
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = InterFontFamily,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val day = input.toIntOrNull()?.coerceIn(MIN_DAY_OF_MONTH, MAX_DAY_OF_MONTH) ?: current
                onConfirm(day)
            }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF191919, heightDp = 900)
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
