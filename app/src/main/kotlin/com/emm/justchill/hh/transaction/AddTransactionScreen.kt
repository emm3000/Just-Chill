package com.emm.justchill.hh.transaction

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.emm.domain.account.Account
import com.emm.domain.category.CategoryType
import com.emm.justchill.components.EmmAmountChill
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PlaceholderOrLabel
import com.emm.justchill.core.theme.PrimaryBlue
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.auth.LabelTextField
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.allColors
import com.emm.justchill.hh.shared.EmmTextInput
import com.emm.justchill.hh.shared.EmmTransactionRadioButton
import com.emm.justchill.hh.transaction.components.EmmCenteredToolbar
import com.emm.justchill.hh.transaction.components.NewAccountItem
import com.emm.justchill.hh.transaction.components.NewButton
import com.emm.justchill.hh.transaction.components.NotesField
import com.emm.justchill.hh.transaction.components.TransactionField
import com.emm.justchill.hh.transaction.components.TransactionTypeToggle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddTransactionScreen(
    vm: AddTransactionViewModel = koinViewModel(),
    onOtherCategorySelected: () -> Unit,
    popBackStack: () -> Unit,
) {

    NewAddTransaction(
        state = vm.state,
        onAction = vm::onAction,
        onOtherCategorySelected = onOtherCategorySelected,
        popBackStack = popBackStack,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewAddTransaction(
    state: AddTransactionUiState,
    onAction: (AddTransactionAction) -> Unit,
    onOtherCategorySelected: () -> Unit,
    popBackStack: () -> Unit,
) {

    val (showAccountPicker, setShowAccountPicker) = rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val kb = LocalSoftwareKeyboardController.current

    val isKeyboardOpen = WindowInsets.isImeVisible

    val datePickerState: DatePickerState = rememberDatePickerState()

    val (showSelectDate, setShowSelectDate) = remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            EmmCenteredToolbar(
                title = "Agregar Transacción",
                modifier = Modifier.fillMaxWidth(),
                navigationIconClick = Icons.Rounded.Close,
                onNavigationIconClick = {
                    kb?.hide()
                    popBackStack()
                },
                actions = {
                    TextButton(
                        onClick = {
                            onAction(AddTransactionAction.OnReset)
                        }
                    ) {
                        Text(
                            modifier = Modifier,
                            text = "Reset",
                            fontFamily = LatoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 17.sp
                        )
                    }
                }
            )
        },
        bottomBar = {
            NewButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                enabled = state.isEnabled,
                onClick = {
                    onAction(AddTransactionAction.OnSave)
                    popBackStack()
                },
                title = "Guardar transacción"
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 15.dp)
                .verticalScroll(scrollState)
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            TransactionTypeToggle(
                selectedType = state.transactionType,
                onTypeSelected = { onAction(AddTransactionAction.OnTransactionTypeChange(it)) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                modifier = Modifier.padding(bottom = 10.dp),
                text = "Ingresa el monto",
                fontFamily = LatoFontFamily,
            )
            EmmAmountChill(
                modifier = Modifier.fillMaxWidth(),
                value = state.amount,
                onValueChange = {
                    onAction(AddTransactionAction.OnAmountChange(it))
                },
                onNext = {
                    kb?.hide()
                    focusManager.clearFocus()
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            CategorySelector(
                categorySelected = state.categorySelected,
                onCategorySelected = {
                    onAction(AddTransactionAction.OnCategorySelected(it))
                },
                categories = state.categories,
                onOtherCategorySelected = onOtherCategorySelected
            )

            Spacer(modifier = Modifier.height(30.dp))

            TransactionField(
                label = "Fecha",
                value = state.date,
                icon = Icons.Rounded.DateRange,
                onClick = {
                    focusManager.clearFocus()
                    setShowSelectDate(true)
                }
            )

            Spacer(modifier = Modifier.height(15.dp))

            TransactionField(
                label = "Cuenta",
                value = state.accountSelected?.name.orEmpty(),
                icon = Icons.Rounded.AccountBalanceWallet,
                onClick = {
                    if (isKeyboardOpen) {
                        scope.launch {
                            focusManager.clearFocus()
                            kb?.hide()
                            delay(300L)
                        }.invokeOnCompletion {
                            setShowAccountPicker(true)
                        }
                    } else {
                        setShowAccountPicker(true)
                    }
                }
            )

            Spacer(modifier = Modifier.height(15.dp))

            NotesField(
                value = state.description,
                onValueChange = { onAction(AddTransactionAction.OnDescriptionChange(it)) },
                onNext = {
                    kb?.hide()
                    focusManager.clearFocus()
                }
            )
        }
    }

    if (showSelectDate) {
        DatePickerDialog(
            onDismissRequest = {
                setShowSelectDate(false)
            },
            confirmButton = {
                OutlinedButton(onClick = {
                    onAction(AddTransactionAction.OnDateChangeInMillis(datePickerState.selectedDateMillis))
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

    BottomSheetDialogForPickAccount(
        setShowAccountPicker = setShowAccountPicker,
        showAccountPicker = showAccountPicker,
        accounts = state.accounts,
        onAction = onAction,
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CategorySelector(
    categorySelected: SelectableCategory?,
    categories: List<SelectableCategory>,
    onCategorySelected: (SelectableCategory) -> Unit,
    onOtherCategorySelected: () -> Unit,
) {

    Text(
        text = "Categoría",
        fontWeight = FontWeight.Bold,
        fontFamily = LatoFontFamily,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(20.dp))

    BoxWithConstraints {
        val columns = 4
        val spacing = 5.dp
        val totalSpacing = spacing * (columns - 1)
        val itemWidth = (maxWidth - totalSpacing) / columns
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            categories.forEach {
                key(it.categoryId) {
                    val isSelected = it.categoryId == categorySelected?.categoryId
                    Column(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(90.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isSelected) Modifier.border(
                                    width = 1.dp,
                                    color = PrimaryBlue,
                                    shape = RoundedCornerShape(10.dp)
                                ) else Modifier
                            )
                            .selectable(
                                selected = isSelected,
                                onClick = { onCategorySelected(it) }
                            )
                            .background(MaterialTheme.colorScheme.surface),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(33.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) Modifier.background(PrimaryBlue)
                                    else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = it.icon.icon,
                                contentDescription = it.name,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = it.name,
                            fontSize = 10.sp
                        )
                    }
                }

            }
            key("other") {
                Column(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onOtherCategorySelected()
                        }
                        .background(MaterialTheme.colorScheme.surface),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(33.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Otros",
                        fontSize = 10.sp
                    )
                }
            }
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionScreen(
    state: AddTransactionUiState,
    onAction: (AddTransactionAction) -> Unit,
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
                    onAction(AddTransactionAction.OnDateChangeInMillis(datePickerState.selectedDateMillis))
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

        val screenWidthDp: Dp = LocalWindowInfo.current.containerDpSize.width
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
                onValueChange = { onAction(AddTransactionAction.OnAmountChange(it)) },
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
                onOptionSelected = { onAction(AddTransactionAction.OnTransactionTypeChange(it)) }
            )
        }

        JustClickableInput(state.accountSelected?.name.orEmpty(), "Cuenta") { setShowAccountPicker(true) }

        EmmTextInput(
            modifier = Modifier,
            label = "En que gaste",
            placeholder = "Ingresa tu gasto",
            value = state.description,
            onChange = { onAction(AddTransactionAction.OnDescriptionChange(it)) },
        )

        JustClickableInput(state.date, "Fecha;") { setShowSelectDate(true) }

        NewButton(
            title = "Guardar",
            onClick = {
                onAction(AddTransactionAction.OnSave)
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
    onAction: (AddTransactionAction) -> Unit
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
                onAccountSelected = { onAction(AddTransactionAction.OnAccountSelected(it)) },
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

        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = "Seleccione una cuenta",
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = LatoFontFamily,
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(20.dp))

        val width = LocalWindowInfo.current.containerDpSize.width
        LazyColumn(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            itemsIndexed(accounts, key = { _, account -> account.accountId }) { index, account ->
                Column {
                    NewAccountItem(
                        modifier = Modifier.fillMaxWidth(),
                        accountName = account.name,
                        balance = account.balance,
                        accountType = account.name,
                    ) {
                        onAccountSelected(account)
                        dismiss()
                    }
                    if (accounts.size == index + 1) return@itemsIndexed
                    HorizontalDivider(
                        modifier = Modifier.requiredWidth(width),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }

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

@Preview
@Composable
private fun NewAddTransactionPreview() {
    EmmTheme {
        val categories = remember {
            buildList {
                repeat(5) {
                    add(
                        SelectableCategory(
                            categoryId = "$it nominavi",
                            name = "$it Ann Chan",
                            icon = AppIconCatalog.catalog[it],
                            color = allColors[it],
                            categoryType = CategoryType.Income
                        )
                    )
                }
            }
        }
        NewAddTransaction(
            state = AddTransactionUiState(
                categories = categories
            ),
            onAction = {},
            popBackStack = {},
            onOtherCategorySelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun IncomePreview() {
    EmmTheme {
        AddTransactionScreen(
            state = AddTransactionUiState(),
            onAction = {},
            popBackStack = {}
        )
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