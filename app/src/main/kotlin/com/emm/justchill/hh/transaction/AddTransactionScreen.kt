package com.emm.justchill.hh.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmButtonVariant
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.modalScreenInsets
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.allColors
import com.emm.justchill.hh.shared.UiStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddTransactionScreen(
    vm: AddTransactionViewModel,
    onOtherCategorySelected: () -> Unit,
    popBackStack: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AddTransactionEffect.TransactionSaved -> popBackStack()
                is AddTransactionEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AddTransactionScreenContent(
        state = state,
        onIntent = vm::onIntent,
        onOtherCategorySelected = onOtherCategorySelected,
        popBackStack = popBackStack,
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionScreenContent(
    state: AddTransactionUiState,
    onIntent: (AddTransactionIntent) -> Unit,
    onOtherCategorySelected: () -> Unit,
    popBackStack: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val isKeyboardOpen = WindowInsets.isImeVisible
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val (showSelectDate, setShowSelectDate) = remember { mutableStateOf(false) }
    val (showAccountPicker, setShowAccountPicker) = rememberSaveable { mutableStateOf(false) }
    val datePickerState: DatePickerState = rememberDatePickerState()
    val amountFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { amountFocus.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .modalScreenInsets(),
    ) {
        AddScreenTopBar(
            title = when (state.transactionType) {
                TransactionType.Income -> "Nuevo ingreso"
                TransactionType.Spend -> "Nuevo gasto"
            },
            showReset = state.hasChanges,
            onClose = {
                keyboard?.hide()
                popBackStack()
            },
            onReset = { onIntent(AddTransactionIntent.OnReset) },
        )

        val sidePadding = Modifier.padding(horizontal = spacing.s4)
        val openAccountPicker: () -> Unit = {
            if (isKeyboardOpen) {
                scope.launch {
                    focusManager.clearFocus()
                    keyboard?.hide()
                    delay(300L)
                }.invokeOnCompletion { setShowAccountPicker(true) }
            } else {
                setShowAccountPicker(true)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(spacing.s6),
        ) {
            Spacer(Modifier.height(spacing.s2))

            Box(modifier = sidePadding) {
                AmountInputSection(
                    amount = state.amount,
                    transactionType = state.transactionType,
                    onAmountChange = { onIntent(AddTransactionIntent.OnAmountChange(it)) },
                    onTypeChange = { onIntent(AddTransactionIntent.OnTransactionTypeChange(it)) },
                    focusRequester = amountFocus,
                    onNext = {
                        keyboard?.hide()
                        focusManager.clearFocus()
                    },
                )
            }

            CategorySelectorSection(
                categories = state.categories,
                selected = state.categorySelected,
                onSelect = { onIntent(AddTransactionIntent.OnCategorySelected(it)) },
                onMore = onOtherCategorySelected,
                contentPadding = PaddingValues(horizontal = spacing.s4),
            )

            Row(
                modifier = sidePadding.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                MetaChip(
                    label = "FECHA",
                    value = state.date,
                    onClick = {
                        focusManager.clearFocus()
                        setShowSelectDate(true)
                    },
                    modifier = Modifier.weight(1f),
                )
                MetaChip(
                    label = "CUENTA",
                    value = state.accountSelected?.name ?: UiStrings.PICK_ACCOUNT,
                    emphasized = state.accountSelected != null,
                    onClick = openAccountPicker,
                    modifier = Modifier.weight(1f),
                )
            }

            Box(modifier = sidePadding) {
                NoteSection(
                    description = state.description,
                    onValueChange = { onIntent(AddTransactionIntent.OnDescriptionChange(it)) },
                )
            }

            Spacer(Modifier.height(spacing.s4))
        }

        EmmButton(
            text = saveButtonLabel(state),
            onClick = { onIntent(AddTransactionIntent.OnSave) },
            enabled = state.isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4),
        )
    }

    if (showSelectDate) {
        DatePickerDialog(
            onDismissRequest = { setShowSelectDate(false) },
            confirmButton = {
                EmmButton(
                    text = "Ok",
                    onClick = {
                        onIntent(AddTransactionIntent.OnDateChangeInMillis(datePickerState.selectedDateMillis))
                        setShowSelectDate(false)
                    },
                )
            },
            dismissButton = {
                EmmButton(
                    text = "Cancelar",
                    onClick = { setShowSelectDate(false) },
                    variant = EmmButtonVariant.Ghost,
                )
            },
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    AccountPickerBottomSheet(
        show = showAccountPicker,
        accounts = state.accounts,
        onAccountSelected = { onIntent(AddTransactionIntent.OnAccountSelected(it)) },
        onDismiss = { setShowAccountPicker(false) },
    )
}

@Composable
private fun AddScreenTopBar(
    title: String,
    showReset: Boolean,
    onClose: () -> Unit,
    onReset: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val closeInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = closeInteraction,
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

        if (showReset) {
            val resetInteraction = remember { MutableInteractionSource() }
            Text(
                text = "Limpiar",
                style = type.labelL,
                color = colors.textSecondary,
                modifier = Modifier
                    .clickable(
                        interactionSource = resetInteraction,
                        indication = null,
                        onClick = onReset,
                    )
                    .padding(spacing.s2),
            )
        }
    }
}

private fun saveButtonLabel(state: AddTransactionUiState): String = when (state.missingField) {
    MissingField.Amount -> "Ingresa un monto"
    MissingField.Account -> "Elige una cuenta"
    null -> when (state.transactionType) {
        TransactionType.Income -> "Anotar ingreso"
        TransactionType.Spend -> "Anotar gasto"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
@Composable
private fun AddTransactionPreview() {
    EmmTheme {
        val categories = remember {
            buildList {
                repeat(5) {
                    add(
                        SelectableCategory(
                            categoryId = CategoryId("$it"),
                            name = "Categoría $it",
                            icon = AppIconCatalog.catalog[it],
                            color = allColors[it],
                            categoryType = CategoryType.Income,
                        )
                    )
                }
            }
        }
        AddTransactionScreenContent(
            state = AddTransactionUiState(categories = categories),
            onIntent = {},
            popBackStack = {},
            onOtherCategorySelected = {},
        )
    }
}
