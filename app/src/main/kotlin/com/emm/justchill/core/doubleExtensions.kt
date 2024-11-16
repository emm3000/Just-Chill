package com.emm.justchill.core

import androidx.compose.ui.text.input.TextFieldValue
import java.math.BigDecimal

fun TextFieldValue.formatInputToDouble(): Double {
    val numericValue: BigDecimal = text
        .replace(",", "")
        .toBigDecimalOrNull()
        ?: BigDecimal.ZERO
    return numericValue.toDouble()
}