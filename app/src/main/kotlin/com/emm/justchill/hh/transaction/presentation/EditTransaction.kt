package com.emm.justchill.hh.transaction.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import com.emm.domain.account.Account
import com.emm.justchill.components.EmmAmountChill
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.shared.shared.EmmDropDown
import com.emm.justchill.hh.shared.shared.EmmPrimaryButton
import com.emm.justchill.hh.shared.shared.EmmTextFieldChill
import com.emm.justchill.hh.shared.shared.EmmTransactionRadioButton
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditTransaction(
    navController: NavController,
    transactionId: String,
    vm: EditTransactionViewModel = koinViewModel(parameters = { parametersOf(transactionId) }),
) {

    val accounts: List<Account> by vm.accounts.collectAsState()

    EditTransaction(
        state = vm.state,
        onAction = vm::onAction,
        navigateUp = { navController.popBackStack() },
        accounts = accounts,
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransaction(
    state: TransactionUiState,
    onAction: (AccountAction) -> Unit,
    navigateUp: () -> Unit = {},
    accounts: List<Account> = emptyList(),
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

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = "Editar Transacción",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = LatoFontFamily,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed {
                        navigateUp()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { setShowDeleteDialog(true) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = DeleteButtonColor,
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {

            EmmDropDown(
                textLabel = "Cuentas",
                textPlaceholder = "Seleccione una cuenta",
                items = accounts,
                itemSelected = state.accountSelected,
                onItemSelected = { onAction(AccountAction.OnAccountSelected(it)) },
                modifier = Modifier.fillMaxWidth(),
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

            EmmTransactionRadioButton(
                modifier = Modifier.fillMaxWidth(),
                selectedOption = state.transactionType,
                onOptionSelected = { onAction(AccountAction.OnTransactionTypeChange(it)) }
            )

            EmmTextFieldChill(
                value = state.description,
                placeholder = "Ingresa una descripción",
                label = "Descripción (opcional)",
                onChange = { onAction(AccountAction.OnDescriptionChange(it)) },
                modifier = Modifier,
            )

            DateInput(state.date) {
                setShowSelectDate(true)
            }

            EmmPrimaryButton(
                text = "Actualizar",
                onClick = {
                    onAction(AccountAction.OnSave)
                    navigateUp()
                },
                enabled = state.isEnabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@PreviewLightDark
@Composable
fun EditTransactionPreview() {
    EmmTheme {
        EditTransaction(
            state = TransactionUiState(),
            onAction = {},
        )
    }
}