package com.emm.justchill.hh.shared

import java.text.NumberFormat
import java.util.*

fun fromCentsToSolesWith(cents: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "PE")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(cents)
}

private val EmptyString: String = String()

val String.Companion.Empty: String
    get() = EmptyString