package com.emm.justchill.hh.presentation.transaction

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.BorderTextFieldColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PlaceholderOrLabel
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.PrimaryDisableButtonColor
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.core.theme.TextDisableColor
import com.emm.justchill.hh.presentation.shared.TextFieldWithLabel
import com.emm.justchill.hh.presentation.shared.TransactionRadioButton
import com.emm.justchill.hh.presentation.auth.LabelTextField
import org.koin.androidx.compose.koinViewModel

@Composable
fun Transaction(
    vm: TransactionViewModel = koinViewModel(),
    navigateToSeeTransactions: () -> Unit,
) {

    Transaction(
        isEnabledButton = vm.isEnabled,
        mountValue = vm.amount,
        onMountChange = vm::updateMount,
        descriptionValue = vm.description,
        onDescriptionChange = vm::updateDescription,
        dateValue = vm.date,
        addTransaction = vm::addTransaction,
        updateDate = vm::updateCurrentDate,
        navigateToSeeTransactions = navigateToSeeTransactions,
        initialTransactionType = vm.transactionType,
        onOptionSelected = vm::updateTransactionType,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Transaction(
    isEnabledButton: Boolean = false,
    mountValue: String = "",
    onMountChange: (String) -> Unit = {},
    descriptionValue: String = "",
    onDescriptionChange: (String) -> Unit = {},
    dateValue: String = "",
    addTransaction: () -> Unit = {},
    updateDate: (Long?) -> Unit = {},
    navigateToSeeTransactions: () -> Unit = {},
    initialTransactionType: TransactionType = TransactionType.INCOME,
    onOptionSelected: (TransactionType) -> Unit = {},
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
                        text = "Agregar gasto",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = LatoFontFamily,
                        color = TextColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed {
                        navigateToSeeTransactions()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = TextColor
                        )
                    }
                },
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(vertical = 10.dp)
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Mount(mountValue, onMountChange)

            TransactionRadioButton(
                modifier = Modifier
                    .fillMaxWidth(),
                selectedOption = initialTransactionType,
                onOptionSelected = onOptionSelected
            )

            TextFieldWithLabel(
                modifier = Modifier
                    .height(140.dp),
                label = "En que gaste",
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
                    addTransaction()
                    navigateToSeeTransactions()
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
                    text = "Guardar",
                    fontFamily = LatoFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
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
        TransactionLabel(text = "Fecha:")
        Spacer(modifier = Modifier.height(5.dp))
        OutlinedTextField(
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
                disabledTextColor = TextColor,
                disabledBorderColor = BorderTextFieldColor,
                disabledPlaceholderColor = BorderTextFieldColor,
                focusedBorderColor = TextColor
            ),
            placeholder = {
                LabelTextField("Fecha")
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
fun Mount(mountValue: String, onMountChange: (String) -> Unit) {

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
                focusedBorderColor = TextColor
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
        Transaction()
    }
}