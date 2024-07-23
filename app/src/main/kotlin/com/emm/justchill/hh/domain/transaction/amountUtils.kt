package com.emm.justchill.hh.domain.transaction

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

fun fromCentsToSoles(cents: Long): BigDecimal {
    return BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
}

fun fromCentsToSolesWith(cents: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "PE")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(cents)
}