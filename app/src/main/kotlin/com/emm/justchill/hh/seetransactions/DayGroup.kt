package com.emm.justchill.hh.seetransactions

import com.emm.justchill.hh.transaction.TransactionUi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DayGroup(val date: LocalDate, val transactions: List<TransactionUi>) {
    val readableDate: String
        get() {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            return when (date) {
                today -> "HOY"

                yesterday -> "AYER"

                else -> {
                    val formatter = DateTimeFormatter.ofPattern(
                        "MMMM dd",
                        Locale.forLanguageTag("es"),
                    )
                    date.format(formatter).uppercase()
                }
            }
        }
}
