package com.emm.justchill.hh.transaction

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.emm.domain.account.Account
import com.emm.justchill.components.EmmAmountChill
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PlaceholderOrLabel
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.auth.LabelTextField
import com.emm.justchill.hh.fasttransaction.AccountItem
import com.emm.justchill.hh.shared.shared.EmmTextInput
import com.emm.justchill.hh.shared.shared.EmmTransactionRadioButton
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun TransactionScreen(
    vm: TransactionViewModel = koinViewModel(),
    popBackStack: () -> Unit,
) {

    TransactionScreen(
        state = vm.state,
        onAction = vm::onAction,
        popBackStack = popBackStack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionScreen(
    state: TransactionUiState,
    onAction: (AccountAction) -> Unit,
    popBackStack: () -> Unit,
) {

    val datePickerState: DatePickerState = rememberDatePickerState()

    val (showSelectDate, setShowSelectDate) = remember {
        mutableStateOf(false)
    }

    val (showAccountPicker, setShowAccountPicker) = rememberSaveable { mutableStateOf(false) }

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
        verticalArrangement = Arrangement.spacedBy(25.dp)
    ) {

        val screenWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp
        EmmCenteredToolbar(
            title = "Agregar Transacción",
            modifier = Modifier.requiredWidth(screenWidthDp),
            navigationIconClick = Icons.Rounded.Close,
            onNavigationIconClick = { popBackStack() }
        )

        Column {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Monto",
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFontFamily,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 17.sp
            )

            EmmAmountChill(
                value = state.amount,
                onValueChange = { onAction(AccountAction.OnAmountChange(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

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

        JustClickableInput(state.accountSelected?.name.orEmpty(), "Cuenta") { setShowAccountPicker(true) }

        EmmTextInput(
            modifier = Modifier,
            label = "En que gaste",
            placeholder = "Ingresa tu gasto",
            value = state.description,
            onChange = { onAction(AccountAction.OnDescriptionChange(it)) },
        )

        JustClickableInput(state.date, "Fecha;") { setShowSelectDate(true) }

        NewButton(
            title = "Guardar",
            onClick = {
                onAction(AccountAction.OnSave)
                popBackStack()
            },
            enabled = state.isEnabled,
            modifier = Modifier.fillMaxWidth()
        )

    }

    BottomSheetDialogForPickAccount(
        setShowAccountPicker = setShowAccountPicker,
        showAccountPicker = showAccountPicker,
        accounts = state.accounts,
        onAction = onAction,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BottomSheetDialogForPickAccount(
    setShowAccountPicker: (Boolean) -> Unit,
    showAccountPicker: Boolean,
    accounts: List<Account>,
    onAction: (AccountAction) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dismiss: () -> Unit = { setShowAccountPicker(false) }

    if (showAccountPicker) {
        ModalBottomSheet(
            onDismissRequest = dismiss,
            sheetState = sheetState,
        ) {
            val view = LocalView.current
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                SideEffect {
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = false
                    insetsController.isAppearanceLightNavigationBars = false
                }
            }
            AccountSelectorContent(
                accounts = accounts,
                onAccountSelected = { onAction(AccountAction.OnAccountSelected(it)) },
                dismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            dismiss()
                        }
                    }
                }
            )
        }

    }
}

@Composable
fun AccountSelectorContent(
    modifier: Modifier = Modifier,
    accounts: List<Account>,
    onAccountSelected: (Account) -> Unit,
    dismiss: () -> Unit,
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = dismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "Seleccione una cuenta",
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = LatoFontFamily,
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(17.dp)
        ) {
            items(accounts, key = Account::accountId) { account ->
                AccountItem(account) {
                    onAccountSelected(it)
                    dismiss()
                }
            }
        }

    }
}

@Preview
@Composable
private fun AccountSelectorContentPreview() {
    EmmTheme {
        AccountSelectorContent(
            modifier = Modifier,
            accounts = listOf(
                Account(
                    accountId = "tantas1",
                    name = "Garrett Owen",
                    balance = 2.3,
                ),
                Account(
                    accountId = "tantas2",
                    name = "Garrett Owen",
                    balance = 2.3,
                ),
                Account(
                    accountId = "tantas3",
                    name = "Garrett Owen",
                    balance = 2.3,
                )
            ),
            onAccountSelected = {},
            dismiss = {}
        )
    }
}

@Composable
fun JustClickableInput(
    value: String,
    label: String,
    onClick: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFontFamily,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 17.sp
        )

        Spacer(modifier = Modifier.height(5.dp))

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledBorderColor = MaterialTheme.colorScheme.onBackground,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = MaterialTheme.colorScheme.onBackground
            ),
            placeholder = {
                Text(
                    text = "Seleccione una cuenta",
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
            },
            textStyle = TextStyle(
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
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

@Preview(showBackground = true)
@Composable
fun IncomePreview() {
    EmmTheme {
        TransactionScreen(
            state = TransactionUiState(),
            onAction = {},
            popBackStack = {}
        )
    }
}