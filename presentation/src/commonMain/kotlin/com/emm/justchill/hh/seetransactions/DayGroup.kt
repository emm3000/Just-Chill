package com.emm.justchill.hh.seetransactions

import com.emm.justchill.hh.shared.SpanishDateFormat
import com.emm.justchill.hh.transaction.TransactionUi
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class DayGroup(val date: LocalDate, val transactions: List<TransactionUi>) {
    val readableDate: String
        get() {
            val zone = TimeZone.currentSystemDefault()
            val today = Clock.System.now().toLocalDateTime(zone).date
            val yesterday = today.minus(1, DateTimeUnit.DAY)
            return when (date) {
                today -> "HOY"

                yesterday -> "AYER"

                else -> SpanishDateFormat.monthDayPadded(date).uppercase()
            }
        }
}
