package com.emm.justchill.hh.transaction.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.components.EmmAmountChill
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PlaceholderOrLabel
import com.emm.justchill.core.theme.TextColor
import com.emm.domain.account.Account
import com.emm.justchill.hh.auth.presentation.LabelTextField
import com.emm.justchill.hh.shared.shared.EmmDropDown
import com.emm.justchill.hh.shared.shared.EmmPrimaryButton
import com.emm.justchill.hh.shared.shared.EmmTextFieldChill
import com.emm.justchill.hh.shared.shared.EmmTransactionRadioButton
import org.koin.androidx.compose.koinViewModel

@Composable
fun TransactionScreen(
    vm: TransactionViewModel = koinViewModel(),
    navigateToSeeTransactions: () -> Unit,
) {

    val accounts: List<Account> by vm.accounts.collectAsState()

    TransactionScreen(
        state = vm.state,
        onAction = vm::onAction,
        accounts = accounts,
        navigateToSeeTransactions = navigateToSeeTransactions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionScreen(
    state: TransactionUiState,
    accounts: List<Account>,
    onAction: (AccountAction) -> Unit,
    navigateToSeeTransactions: () -> Unit,
) {

    val datePickerState: DatePickerState = rememberDatePickerState()

    val (showSelectDate, setShowSelectDate) = remember {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {

        EmmCenteredToolbar(
            title = "Agregar gasto",
            modifier = Modifier.fillMaxWidth(),
        )

        EmmDropDown(
            textLabel = "Cuentas",
            textPlaceholder = "Seleccionar cuenta",
            items = accounts,
            itemSelected = state.accountSelected,
            onItemSelected = { onAction(AccountAction.OnAccountSelected(it)) },
            modifier = Modifier.fillMaxWidth(),
        )

        EmmAmountChill(
            value = state.amount,
            onValueChange = { onAction(AccountAction.OnAmountChange(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        EmmTransactionRadioButton(
            modifier = Modifier
                .fillMaxWidth(),
            selectedOption = state.transactionType,
            onOptionSelected = { onAction(AccountAction.OnTransactionTypeChange(it)) }
        )

        EmmTextFieldChill(
            modifier = Modifier,
            label = "En que gaste",
            placeholder = "Ingresa tu gasto",
            value = state.description,
            onChange = { onAction(AccountAction.OnDescriptionChange(it)) },
        )

        DateInput(state.date) {
            setShowSelectDate(true)
        }

        EmmPrimaryButton(
            text = "Guardar",
            onClick = {
                onAction(AccountAction.OnSave)
                navigateToSeeTransactions()
            },
            enabled = state.isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp)
        )
    }
}

@Composable
fun DateInput(
    dateValue: String,
    showDatePicker: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showDatePicker()
                },
            value = dateValue,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledBorderColor = MaterialTheme.colorScheme.onBackground,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = MaterialTheme.colorScheme.onBackground
            ),
            label = {
                Text(
                    text = "Fecha:",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            },
            textStyle = TextStyle(
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp
            )
        )
    }
}

@Composable
fun Amount(mountValue: String, onMountChange: (String) -> Unit) {

    Column(Modifier.fillMaxWidth()) {

        TransactionLabel("Cantidad:")

        Spacer(modifier = Modifier.height(5.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = mountValue,
            onValueChange = { value ->
                val filter: String = value.filter { it.isDigit() || it == '.' }
                onMountChange(filter)
            },
            placeholder = {
                LabelTextField("Cantidad")
            },
            prefix = {
                Text(
                    text = "S/ ",
                    fontWeight = FontWeight.Normal,
                    fontFamily = LatoFontFamily,
                    color = PlaceholderOrLabel,
                    fontSize = 18.sp
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.onBackground
            ),
            textStyle = TextStyle(
                color = TextColor,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp
            )
        )
    }
}

@Composable
fun TransactionLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Normal,
        fontFamily = LatoFontFamily,
        color = PlaceholderOrLabel,
        fontSize = 17.sp
    )
}

@PreviewLightDark
@Composable
fun IncomePreview() {
    EmmTheme {
        TransactionScreen(
            state = TransactionUiState(),
            accounts = emptyList(),
            onAction = {},
            navigateToSeeTransactions = {}
        )
    }
}