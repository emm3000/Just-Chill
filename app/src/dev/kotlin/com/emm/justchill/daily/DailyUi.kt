package com.emm.justchill.daily

import com.emm.justchill.hh.domain.shared.fromCentsToSolesWith
import com.emm.justchill.hh.presentation.transaction.DateUtils
import com.emm.justchill.daily.domain.Daily
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

data class DailyUi(
    val dailyId: String,
    val amount: String,
    val dailyDate: Long,
    val driverId: Long,
    val readableTime: String,
    val day: String,
    val dayNumber: String,
)

fun Daily.toUi(): DailyUi {

    val date: LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dailyDate), ZoneId.systemDefault())

    return DailyUi(
        dailyId = dailyId,
        amount = "S/ ${fromCentsToSolesWith(amount)}",
        dailyDate = dailyDate,
        driverId = driverId,
        readableTime = DateUtils.readableTime(dailyDate),
        day = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).uppercase(),
        dayNumber = date.dayOfMonth.toString()
    )
}