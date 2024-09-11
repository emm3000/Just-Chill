package com.emm.justchill.hh.domain.shared

import java.text.NumberFormat
import java.util.*

fun fromCentsToSolesWith(cents: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "PE")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(cents)
}