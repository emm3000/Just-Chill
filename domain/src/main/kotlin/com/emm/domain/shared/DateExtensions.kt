package com.emm.domain.shared

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

fun startOfDayDaysAgo(days: Int, clock: Clock, zone: TimeZone): String {
    val today = clock.now().toLocalDateTime(zone).date
    return today.minus(days, DateTimeUnit.DAY).toString()
}
