package com.emm.justchill.loans.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.BorderTextFieldColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PlaceholderOrLabel
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.PrimaryDisableButtonColor
import com.emm.justchill.core.theme.TextColor

@Composable
fun Loans() {

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
                text = "Loans register",
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

            LoansAmountTextField()
            LoansInterestTextField()
            LoansStartDateTextField()
            LoansTextField("Duration")
            LoansTextField("Payment frequency")

            Spacer(modifier = Modifier.height(15.dp))

            Button(
                onClick = {},
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
                    text = "Save",
                    fontFamily = LatoFontFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

    }
}

@Composable
private fun LoansAmountTextField(modifier: Modifier = Modifier) {

    val (text, setText) = remember {
        mutableStateOf("")
    }

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
                "Amount",
                color = PlaceholderOrLabel.copy(alpha = 0.5f)
            )
        },
        label = { LoansLabelOrPlaceHolder("Amount (S/)") },
        prefix = { LoansLabelOrPlaceHolder("S/ ", TextColor) },
        textStyle = loansTextStyle()
    )

}

@Composable
private fun LoansInterestTextField(modifier: Modifier = Modifier) {

    val (text, setText) = remember {
        mutableStateOf("")
    }

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
                "20",
                color = PlaceholderOrLabel.copy(alpha = 0.5f)
            )
        },
        label = { LoansLabelOrPlaceHolder("Interest (%)") },
        prefix = { LoansLabelOrPlaceHolder("%", TextColor) },
        textStyle = loansTextStyle()
    )

}

@Composable
private fun LoansStartDateTextField(modifier: Modifier = Modifier) {

    val (text, setText) = remember {
        mutableStateOf("10 de Julio de 2024")
    }

    TextField(
        modifier = Modifier.fillMaxWidth()
            .clickable {

            },
        value = text,
        onValueChange = setText,
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
                "Start date",
                color = PlaceholderOrLabel.copy(alpha = 0.5f)
            )
        },
        label = { LoansLabelOrPlaceHolder("Start date") },
        textStyle = loansTextStyle(),
        readOnly = true,
        enabled = false,
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
fun LoansPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Loans()
    }
}