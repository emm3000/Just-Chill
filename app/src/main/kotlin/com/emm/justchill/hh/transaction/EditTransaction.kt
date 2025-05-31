package com.emm.justchill.hh.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.emm.justchill.components.EmmAmountChill
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.shared.shared.EmmPrimaryButton
import com.emm.justchill.hh.shared.shared.EmmTextInput
import com.emm.justchill.hh.shared.shared.EmmTransactionRadioButton
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditTransaction(
    navController: NavController,
    transactionId: String,
    vm: EditTransactionViewModel = koinViewModel(parameters = { parametersOf(transactionId) }),
) {

    EditTransaction(
        state = vm.state,
        onAction = vm::onAction,
        navigateUp = { navController.navigateUp() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransaction(
    state: TransactionUiState,
    onAction: (AccountAction) -> Unit,
    navigateUp: () -> Unit = {},
) {

    val datePickerState: DatePickerState = rememberDatePickerState()

    val (showSelectDate, setShowSelectDate) = remember {
        mutableStateOf(false)
    }

    val (showDeleteDialog, setShowDeleteDialog) = remember {
        mutableStateOf(false)
    }

    if (showSelectDate) {
        DatePickerDialog(
            onDismissRequest = {
                setShowSelectDate(false)
            },
            confirmButton = {
                OutlinedButton(onClick = {
                    onAction(AccountAction.OnDateChangeInMillis(datePickerState.selectedDateMillis))
                    setShowSelectDate(false)
                }) {
                    Text(text = "Ok")
                }
            },
            dismissButton = {
                Button(onClick = { setShowSelectDate(false) }) {
                    Text(text = "Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    if (showDeleteDialog) {
        EmmDeleteDialog(
            setShowDeleteDialog = setShowDeleteDialog,
            onConfirmButton = {
                setShowDeleteDialog(false)
                onAction(AccountAction.OnDelete)
                navigateUp()
            }
        )
    }

    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(25.dp)
    ) {

        val screenWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp
        EmmCenteredToolbar(
            title = "Agregar Transacción",
            modifier = Modifier.requiredWidth(screenWidthDp),
            navigationIconClick = Icons.Rounded.Close,
            onNavigationIconClick = {
                keyboard?.hide()
                navigateUp()
            },
            actions = {
                IconButton(onClick = {
                    keyboard?.hide()
                    setShowDeleteDialog(true)
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = DeleteButtonColor
                    )
                }
            }
        )

        Text(
            text = "Ingrese un monto",
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )

        EmmAmountChill(
            value = state.amount,
            onValueChange = { onAction(AccountAction.OnAmountChange(it)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Tipo: ",
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFontFamily,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 17.sp

            )
            EmmTransactionRadioButton(
                modifier = Modifier
                    .fillMaxWidth(),
                selectedOption = state.transactionType,
                onOptionSelected = { onAction(AccountAction.OnTransactionTypeChange(it)) }
            )
        }

        EmmTextInput(
            value = state.description,
            placeholder = "Ingresa una descripción",
            label = "Descripción (opcional)",
            onChange = { onAction(AccountAction.OnDescriptionChange(it)) },
            modifier = Modifier,
        )

        JustClickableInput(state.date, "Fecha: ") {
            setShowSelectDate(true)
        }

        EmmPrimaryButton(
            text = "Actualizar",
            onClick = {
                keyboard?.hide()
                onAction(AccountAction.OnSave)
                navigateUp()
            },
            enabled = state.isEnabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditTransactionPreview() {
    EmmTheme {
        EditTransaction(
            state = TransactionUiState(),
            onAction = {},
        )
    }
}