package com.emm.justchill.hh.presentation.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.PrimaryDisableButtonColor
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.core.theme.TextDisableColor
import com.emm.justchill.hh.domain.account.Account
import com.emm.justchill.hh.presentation.shared.DropDownContainer
import com.emm.justchill.hh.presentation.shared.TextFieldWithLabel
import com.emm.justchill.hh.presentation.shared.TransactionRadioButton
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditTransaction(
    navController: NavController,
    transactionId: String,
    vm: EditTransactionViewModel = koinViewModel(parameters = { parametersOf(transactionId) })
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
        accountLabel = vm.accountLabel,
        onAccountLabelChange = vm::updateAccountLabel,
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
    onAccountChange: (Account) -> Unit = {},
    accountLabel: String = "",
    onAccountLabelChange: (String) -> Unit = {},
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
        DeleteDialog(
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
                    containerColor = BackgroundColor
                ),
                title = {
                    Text(
                        text = "Editar Transacción",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = LatoFontFamily,
                        color = TextColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed {
                        navigateUp()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = TextColor
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
                .background(BackgroundColor)
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp)
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {

            DropDownContainer(
                account = accounts,
                onAccountChange = onAccountChange,
                text = accountLabel,
                setText = onAccountLabelChange
            )

            Amount(mountValue, onMountChange)

            TransactionRadioButton(
                modifier = Modifier.fillMaxWidth(),
                selectedOption = initialTransactionType,
                onOptionSelected = onOptionSelected
            )

            TextFieldWithLabel(
                modifier = Modifier,
                label = "Descripción (opcional)",
                placeholder = "Ingresa una descripción",
                value = descriptionValue,
                onChange = onDescriptionChange
            )

            DateInput(dateValue) {
                setShowSelectDate(true)
            }

            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onClick = dropUnlessResumed {
                    updateTransaction()
                    navigateUp()
                },
                enabled = isEnabledButton,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = PrimaryButtonColor,
                    disabledContainerColor = PrimaryDisableButtonColor,
                    contentColor = TextColor,
                    disabledContentColor = TextDisableColor
                ),
                shape = RoundedCornerShape(25)
            ) {
                Text(
                    text = "Actualizar",
                    fontFamily = LatoFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }
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

@Preview(showBackground = true)
@Composable
fun EditTransactionPreview() {
    EmmTheme {
        EditTransaction()
    }
}