package com.emm.justchill.me.loan.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.BorderTextFieldColor
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PlaceholderOrLabel
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.PrimaryDisableButtonColor
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.me.driver.domain.Driver
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddLoanScreen(
    driverId: Long,
    navigateToBack: () -> Unit,
    vm: AddLoanViewModel = koinViewModel(parameters = { parametersOf(driverId) }),
) {

    val driver: Driver? by vm.currentDriver.collectAsState()

    AddLoanScreen(
        driver = driver,
        updateDate = vm::updateStartDate,
        dateValue = vm.startDate,
        amount = vm.amount,
        updateAmount = vm::updateAmount,
        interestValue = vm.interest,
        updateInterest = vm::updateInterest,
        durationValue = vm.duration,
        updateDuration = vm::updateDuration,
        onSave = vm::create,
        navigateToBack = navigateToBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLoanScreen(
    driver: Driver? = null,
    dateValue: String = "",
    updateDate: (Long?) -> Unit = {},
    amount: String = "",
    updateAmount: (String) -> Unit = {},
    interestValue: String = "",
    updateInterest: (String) -> Unit = {},
    durationValue: String = "",
    updateDuration: (String) -> Unit = {},
    onSave: () -> Unit = {},
    navigateToBack: () -> Unit = {},
) {

    val (showSelectDate, setShowSelectDate) = remember {
        mutableStateOf(false)
    }

    val datePickerState: DatePickerState = rememberDatePickerState()

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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Prestamos (${driver?.name.orEmpty()})",
                    color = TextColor,
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                LoansAmountTextField(
                    text = amount,
                    setText = updateAmount
                )
                LoansInterestTextField(
                    text = interestValue,
                    setText = updateInterest
                )
                LoansStartDateTextField(
                    text = dateValue,
                    onTouch = { setShowSelectDate(true) }
                )

                LoansDurationTextField(
                    text = durationValue,
                    setText = updateDuration
                )

                Spacer(modifier = Modifier.height(15.dp))

                val keyboardController = LocalSoftwareKeyboardController.current
                Button(
                    onClick = dropUnlessResumed {
                        keyboardController?.hide()
                        onSave()
                        navigateToBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryButtonColor,
                        disabledContainerColor = PrimaryDisableButtonColor
                    ),
                    shape = RoundedCornerShape(20)
                ) {
                    Text(
                        text = "Guardar",
                        fontFamily = LatoFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
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
                    text = "Ok",
                    fontSize = 16.sp,
                    color = DeleteButtonColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LatoFontFamily
                )
            }

        },
    )
}

@Composable
private fun LoansAmountTextField(
    text: String,
    setText: (String) -> Unit,
) {

    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = { value ->
            val filter: String = value.filter(Char::isDigit)
            setText(filter)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = PrimaryButtonColor
        ),
        placeholder = {
            LoansLabelOrPlaceHolder(
                "Ingrese el monto",
                color = PlaceholderOrLabel.copy(alpha = 0.5f)
            )
        },
        label = { LoansLabelOrPlaceHolder("Monto") },
        prefix = { LoansLabelOrPlaceHolder("S/ ", TextColor) },
        textStyle = loansTextStyle()
    )

}

@Composable
private fun LoansInterestTextField(
    text: String,
    setText: (String) -> Unit,
) {

    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = { value ->
            val filter: String = value.filter(Char::isDigit)
            val number: Int = filter.toIntOrNull() ?: return@TextField setText("")
            val isInRange: Boolean = number in 1..100
            if (isInRange) setText(filter)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = PrimaryButtonColor
        ),
        placeholder = {
            LoansLabelOrPlaceHolder(
                "Ingrese el interés, 1 . . 100 %",
                color = PlaceholderOrLabel.copy(alpha = 0.5f)
            )
        },
        label = { LoansLabelOrPlaceHolder("Interés") },
        prefix = { LoansLabelOrPlaceHolder("%", TextColor) },
        textStyle = loansTextStyle()
    )

}

@Composable
private fun LoansStartDateTextField(
    text: String,
    onTouch: () -> Unit,
) {

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTouch),
        value = text,
        onValueChange = {},
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            disabledIndicatorColor = BorderTextFieldColor,
            focusedIndicatorColor = PrimaryButtonColor
        ),
        placeholder = {
            LoansLabelOrPlaceHolder(
                "Fecha de inicio",
                color = PlaceholderOrLabel.copy(alpha = 0.5f)
            )
        },
        label = { LoansLabelOrPlaceHolder("Fecha de inicio") },
        textStyle = loansTextStyle(),
        readOnly = true,
        enabled = false,
    )
}

@Composable
private fun LoansDurationTextField(
    text: String,
    setText: (String) -> Unit,
) {

    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = { value ->
            value.filter(Char::isDigit).also(setText)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = PrimaryButtonColor
        ),
        placeholder = {
            LoansLabelOrPlaceHolder(
                "En cuantos días?",
                color = PlaceholderOrLabel.copy(alpha = 0.5f)
            )
        },
        label = { LoansLabelOrPlaceHolder("Días") },
        textStyle = loansTextStyle()
    )
}

@Composable
private fun LoansTextField(
    label: String,
    prefix: String = "",
    suffix: String = "",
) {

    val (text, setText) = remember {
        mutableStateOf("")
    }

    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = setText,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = PrimaryButtonColor
        ),
        placeholder = {
            LoansLabelOrPlaceHolder(
                label,
                color = PlaceholderOrLabel.copy(alpha = 0.5f)
            )
        },
        label = { LoansLabelOrPlaceHolder(label) },
        prefix = { LoansLabelOrPlaceHolder(prefix, TextColor) },
        suffix = { LoansLabelOrPlaceHolder(suffix, TextColor) },
        textStyle = loansTextStyle()
    )
}

private fun loansTextStyle() = TextStyle(
    fontFamily = LatoFontFamily,
    color = TextColor,
    fontSize = 18.sp,
    fontWeight = FontWeight.Normal
)

@Composable
private fun LoansLabelOrPlaceHolder(
    value: String,
    color: Color = PlaceholderOrLabel,
) {
    Text(
        text = value,
        color = color,
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    )
}

@Preview(showBackground = true)
@Composable
fun LoansPreview() {
    EmmTheme {
        AddLoanScreen(
            dateValue = "gaa"
        )
    }
}