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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.shared.shared.EmmDropDown
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

    val accounts: List<Account> by vm.accounts.collectAsState()

    EditTransaction(
        isEnabledButton = vm.isEnabled,
        mountValue = vm.amount,
        onMountChange = vm::updateMount,
        descriptionValue = vm.description,
        onDescriptionChange = vm::updateDescription,
        dateValue = vm.date,
        updateTransaction = vm::updateTransaction,
        updateDate = vm::updateCurrentDate,
        navigateUp = { navController.popBackStack() },
        initialTransactionType = vm.transactionType,
        onOptionSelected = vm::updateTransactionType,
        deleteTransaction = vm::deleteTransaction,
        accounts = accounts,
        onAccountChange = vm::updateAccountSelected,
        accountSelected = vm.accountSelected,
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransaction(
    isEnabledButton: Boolean = false,
    mountValue: String = "",
    onMountChange: (String) -> Unit = {},
    descriptionValue: String = "",
    onDescriptionChange: (String) -> Unit = {},
    dateValue: String = "",
    updateTransaction: () -> Unit = {},
    updateDate: (Long?) -> Unit = {},
    navigateUp: () -> Unit = {},
    initialTransactionType: TransactionType = TransactionType.INCOME,
    onOptionSelected: (TransactionType) -> Unit = {},
    deleteTransaction: () -> Unit = {},
    accounts: List<Account> = emptyList(),
    accountSelected: Account? = null,
    onAccountChange: (Account) -> Unit = {},
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
                    updateDate(datePickerState.selectedDateMillis)
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
                deleteTransaction()
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp)
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {

            EmmDropDown(
                textLabel = "Cuentas",
                textPlaceholder = "Seleccione una cuenta",
                items = accounts,
                itemSelected = accountSelected,
                onItemSelected = onAccountChange,
                modifier = Modifier.fillMaxWidth(),
            )

            EmmTextAmount(
                value = mountValue,
                onAmountChange = onMountChange,
                label = "Cantidad",
                placeholder = "Ingrese una cantidad",
                modifier = Modifier.fillMaxWidth(),
            )

            EmmTransactionRadioButton(
                modifier = Modifier.fillMaxWidth(),
                selectedOption = initialTransactionType,
                onOptionSelected = onOptionSelected
            )

            EmmTextInput(
                modifier = Modifier,
                label = "Descripción (opcional)",
                placeholder = "Ingresa una descripción",
                value = descriptionValue,
                onChange = onDescriptionChange
            )

            DateInput(dateValue) {
                setShowSelectDate(true)
            }

            EmmPrimaryButton(
                text = "Actualizar",
                onClick = {
                    updateTransaction()
                    navigateUp()
                },
                enabled = isEnabledButton,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DeleteDialog(
    setShowDeleteDialog: (Boolean) -> Unit,
    onConfirmButton: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { setShowDeleteDialog(false) },
        containerColor = BackgroundColor,
        text = {
            Text(
                text = "Estas seguro de eliminar esta transacción.",
                color = TextColor,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            )
        },
        title = {
            Text(
                text = "Eliminar transacción",
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Black,
                color = TextColor
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirmButton()
            }) {
                Text(
                    text = "Confirmar",
                    fontSize = 16.sp,
                    color = DeleteButtonColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LatoFontFamily
                )
            }

        },
        dismissButton = {
            TextButton(onClick = { setShowDeleteDialog(false) }) {
                Text(
                    text = "Cancelar",
                    fontSize = 16.sp,
                    color = TextColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LatoFontFamily
                )
            }
        }
    )
}

@PreviewLightDark
@Composable
fun EditTransactionPreview() {
    EmmTheme {
        EditTransaction()
    }
}