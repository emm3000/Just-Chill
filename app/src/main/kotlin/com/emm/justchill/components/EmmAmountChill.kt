package com.emm.justchill.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.HhBackgroundColor
import com.emm.justchill.core.theme.HhOnBackgroundColor
import java.math.BigDecimal
import java.text.DecimalFormat

@Composable
fun EmmAmountChill(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
) {

    val amount = value.text.replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
    val textColor = when {
        amount == BigDecimal("0.00") -> {
            HhOnBackgroundColor.copy(alpha = 0.5f)
        }
        amount < BigDecimal("1.00") -> DeleteButtonColor
        amount >= BigDecimal("1.00") -> HhOnBackgroundColor
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    }

    AmountTextField(
        value = value,
        onValueChange = { newValue ->
            val formattedValue: TextFieldValue = formatInputToAmount(newValue)
            onValueChange(formattedValue)
        },
        textColor = textColor,
        modifier = modifier,
    )
}



@Composable
fun AmountTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {

    BasicTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(
            fontSize = 40.sp,
            color = textColor,
            fontFamily = FontFamily.SansSerif,
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "S/  ",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 40.sp,
                    color = textColor
                )
                innerTextField()
            }
        },
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
    )
}

fun formatInputToAmount(input: TextFieldValue): TextFieldValue {
    val filteredText: String = input.text.filter { it.isDigit() }
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
fun AmountPreview() {
    EmmTheme {
        Surface(modifier = Modifier.background(HhBackgroundColor)) {
            EmmAmountChill(
                value = TextFieldValue("100.00"),
                onValueChange = {},
                modifier = Modifier.background(HhBackgroundColor)
            )
        }
    }
}