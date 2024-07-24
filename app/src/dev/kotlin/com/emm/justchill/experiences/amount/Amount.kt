package com.emm.justchill.experiences.amount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import java.math.BigDecimal
import java.text.DecimalFormat

@Composable
fun Amount() {

    var input by remember { mutableStateOf(TextFieldValue(text = "0.00")) }
    var textColor by remember { mutableStateOf(Color.Gray) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AmountTextField(
            value = input,
            onValueChange = { newValue ->
                val formattedValue = formatInputToAmount2(newValue)
                input = formattedValue

                val amount = formattedValue.text.replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
                textColor = when {
                    amount == BigDecimal("0.00") -> {
                        Color.Gray
                    }
                    amount < BigDecimal("1.00") -> Color.Red
                    amount >= BigDecimal("1.00") -> Color.Blue
                    else -> Color.Gray
                }
            },
            textColor = textColor
        )
    }
}

@Composable
fun AmountTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textColor: Color
) {
    BasicTextField(
        modifier = Modifier,
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(
            fontSize = 40.sp,
            color = textColor
        ),
        decorationBox = { innerTextField ->
            Box(
                Modifier
                    .background(Color.Transparent)
                    .padding(16.dp)
            ) {
                innerTextField()
            }
        }
    )
}

fun formatInputToAmount2(input: TextFieldValue): TextFieldValue {
    val filteredText = input.text.filter { it.isDigit() }
    val amount: Long = if (filteredText.isEmpty()) {
        0
    } else {
        filteredText.toLong()
    }
    val decimalFormat = DecimalFormat("#,##0.00")
    val formattedAmount = decimalFormat.format(amount / 100.0)
    return input.copy(text = formattedAmount, selection = TextRange(formattedAmount.length))
}

@Preview(showBackground = true)
@Composable
fun AmountPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Amount()
    }
}