package com.emm.justchill.experiences.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

val daysOfWeek: List<DayOfWeek> = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
)

fun calculateDays(month: LocalDate): List<List<LocalDate?>> {

    val currentMonth: YearMonth = YearMonth.from(month)
    val firstDayOfMonth: LocalDate = currentMonth.atDay(1)
    val lastDayOfMonth: LocalDate = currentMonth.atEndOfMonth()

    val totalDays: List<LocalDate> = (1..lastDayOfMonth.dayOfMonth).map(currentMonth::atDay)
    val startOffset: Int = firstDayOfMonth.dayOfWeek.value % 7

    val calendarRows: List<List<LocalDate?>> = (0 until 5).map { weekIndex ->
        val startDay: Int = weekIndex * 7 - startOffset + 1
        (startDay..(startDay + 6)).map { day ->
            totalDays.getOrNull(day - 1)
        }
    }

    return calendarRows
}