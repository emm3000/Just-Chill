package com.emm.justchill.core.presentation.date

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

enum class DateShortcutKind { Today, Yesterday, ThisWeek, ThisMonth }

data class DateShortcut(val kind: DateShortcutKind, val date: LocalDate) {

    val label: String get() = when (kind) {
        DateShortcutKind.Today -> "Hoy"
        DateShortcutKind.Yesterday -> "Ayer"
        DateShortcutKind.ThisWeek -> "Esta semana"
        DateShortcutKind.ThisMonth -> "Este mes"
    }

    fun isActiveFor(day: LocalDate): Boolean = kind == DateShortcutKind.Today && day == date
}

fun dateShortcutsOf(today: LocalDate): List<DateShortcut> = listOf(
    DateShortcut(DateShortcutKind.Today, today),
    DateShortcut(DateShortcutKind.Yesterday, today.minus(1, DateTimeUnit.DAY)),
    DateShortcut(DateShortcutKind.ThisWeek, today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)),
    DateShortcut(DateShortcutKind.ThisMonth, LocalDate(today.year, today.month, 1)),
)
