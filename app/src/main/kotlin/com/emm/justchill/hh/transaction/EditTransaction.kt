package com.emm.justchill.hh.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmButtonVariant
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditTransaction(
    transactionId: String,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    vm: EditTransactionViewModel = koinViewModel(parameters = { parametersOf(transactionId) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                EditTransactionEffect.TransactionUpdated -> onBack()
                EditTransactionEffect.TransactionDeleted -> onBack()
                is EditTransactionEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    EditTransactionContent(
        state = state,
        onIntent = vm::onIntent,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EditTransactionContent(
    state: EditTransactionUiState,
    onIntent: (EditTransactionIntent) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val isKeyboardOpen = WindowInsets.isImeVisible
    val scope = rememberCoroutineScope()

    val (showSelectDate, setShowSelectDate) = remember { mutableStateOf(false) }
    val (showAccountPicker, setShowAccountPicker) = rememberSaveable { mutableStateOf(false) }
    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }

    val datePickerState: DatePickerState = rememberDatePickerState()
    val amountFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { amountFocus.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding(),
    ) {

        TopBar(
            title = "Editar",
            onClose = {
                keyboard?.hide()
                onBack()
            },
            onDelete = {
                keyboard?.hide()
                setShowDeleteDialog(true)
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s6),
        ) {

            Spacer(Modifier.height(spacing.s2))

            TypeToggle(
                selected = state.transactionType,
                onSelect = { onIntent(EditTransactionIntent.OnTransactionTypeChange(it)) },
            )

            AmountHeroInput(
                value = state.amount,
                onValueChange = { onIntent(EditTransactionIntent.OnAmountChange(it)) },
                type = state.transactionType,
                focusRequester = amountFocus,
                onNext = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
            )

            ClickableRow(
                label = "FECHA",
                value = state.date,
                onClick = {
                    focusManager.clearFocus()
                    setShowSelectDate(true)
                },
            )

            ClickableRow(
                label = "CUENTA",
                value = state.accountSelected?.name ?: "Selecciona una cuenta",
                emphasized = state.accountSelected != null,
                onClick = {
                    if (isKeyboardOpen) {
                        scope.launch {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            delay(300L)
                        }.invokeOnCompletion { setShowAccountPicker(true) }
                    } else {
                        setShowAccountPicker(true)
                    }
                },
            )

            EmmTextInput(
                value = state.description,
                onValueChange = { onIntent(EditTransactionIntent.OnDescriptionChange(it)) },
                label = "DESCRIPCIÓN",
                placeholder = "Opcional",
                singleLine = false,
            )

            Spacer(Modifier.height(spacing.s4))
        }

        EmmButton(
            text = "Actualizar",
            onClick = { onIntent(EditTransactionIntent.OnSave) },
            enabled = state.isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.s4)
                .navigationBarsPadding(),
        )
    }

    if (showSelectDate) {
        DatePickerDialog(
            onDismissRequest = { setShowSelectDate(false) },
            confirmButton = {
                EmmButton("Ok", onClick = {
                    onIntent(EditTransactionIntent.OnDateChangeInMillis(datePickerState.selectedDateMillis))
                    setShowSelectDate(false)
                })
            },
            dismissButton = {
                EmmButton("Cancelar", onClick = { setShowSelectDate(false) }, variant = EmmButtonVariant.Ghost)
            },
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { setShowDeleteDialog(false) },
            title = { Text("Eliminar transacción") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                EmmButton(
                    "Eliminar",
                    onClick = {
                        setShowDeleteDialog(false)
                        onIntent(EditTransactionIntent.OnDelete)
                    },
                    variant = EmmButtonVariant.Destructive,
                )
            },
            dismissButton = {
                EmmButton(
                    "Cancelar",
                    onClick = { setShowDeleteDialog(false) },
                    variant = EmmButtonVariant.Ghost,
                )
            },
            containerColor = colors.surface2,
        )
    }

    BottomSheetDialogForPickAccount(
        setShowAccountPicker = setShowAccountPicker,
        showAccountPicker = showAccountPicker,
        accounts = state.accounts,
        onAccountSelected = { onIntent(EditTransactionIntent.OnAccountSelected(it)) },
    )
}

@Composable
private fun TopBar(title: String, onClose: () -> Unit, onDelete: () -> Unit) {
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
        val deleteInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = deleteInteraction,
                    indication = null,
                    onClick = onDelete,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Eliminar",
                tint = colors.danger,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
@Composable
private fun EditTransactionPreview() {
    EmmTheme {
        EditTransactionContent(
            state = EditTransactionUiState(),
            onIntent = {},
            onBack = {},
        )
    }
}
