package com.emm.justchill.quota

import com.emm.justchill.hh.domain.shared.fromCentsToSolesWith
import com.emm.justchill.hh.presentation.transaction.DateUtils
import com.emm.justchill.quota.domain.Quota
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

data class QuotaUi(
    val quoteId: String,
    val amount: String,
    val quoteDate: Long,
    val driverId: Long,
    val readableTime: String,
    val day: String,
    val dayNumber: String,
)

fun Quota.toUi(): QuotaUi {

    val date: LocalDateTime = LocalDateTime
        .ofInstant(Instant.ofEpochMilli(quoteDate), ZoneId.systemDefault())

    return QuotaUi(
        quoteId = quoteId,
        amount = "S/ ${fromCentsToSolesWith(amount)}",
        quoteDate = quoteDate,
        driverId = driverId,
        readableTime = DateUtils.readableTime(quoteDate),
        day = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).uppercase(),
        dayNumber = date.dayOfMonth.toString()
    )
}