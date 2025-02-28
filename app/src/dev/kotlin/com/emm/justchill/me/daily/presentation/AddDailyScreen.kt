package com.emm.justchill.me.daily.presentation

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.PrimaryDisableButtonColor
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.core.theme.TextDisableColor
import com.emm.justchill.hh.transaction.presentation.Amount
import com.emm.justchill.hh.transaction.presentation.DateInput
import com.emm.justchill.me.driver.domain.Driver
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddDailyScreen(
    driverId: Long,
    vm: AddDailyViewModel = koinViewModel(
        parameters = { parametersOf(driverId) }
    ),
    navigateToBack: () -> Unit,
) {

    val currentDriver: Driver? by vm.currentDriver.collectAsState()

    AddDailyScreen(
        currentDriver = currentDriver,
        isEnabledButton = vm.isEnabled,
        dateValue = vm.date,
        updateDate = vm::updateCurrentDate,
        currentAmount = vm.amount,
        updateAmount = vm::updateAmount,
        saveAction = vm::addDaily,
        navigateToBack = navigateToBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDailyScreen(
    currentDriver: Driver? = null,
    isEnabledButton: Boolean = false,
    dateValue: String = "",
    currentAmount: String = "",
    updateAmount: (String) -> Unit = {},
    updateDate: (Long?) -> Unit = {},
    saveAction: () -> Unit,
    navigateToBack: () -> Unit = {},
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {

        Text(
            modifier = Modifier,
            text = currentDriver?.name.orEmpty(),
            color = TextColor,
            fontFamily = LatoFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Amount(currentAmount, updateAmount)

        DateInput(dateValue) {
            setShowSelectDate(true)
        }

        val keyboardController = LocalSoftwareKeyboardController.current

        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            onClick = dropUnlessResumed {
                keyboardController?.hide()
                saveAction()
                navigateToBack()
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

@Preview(showBackground = true)
@Composable
private fun AddDailyScreenPreview() {
    EmmTheme {
    }
}