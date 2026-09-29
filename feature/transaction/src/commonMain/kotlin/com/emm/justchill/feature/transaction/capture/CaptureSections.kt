package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.presentation.category.SelectableCategory
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

internal fun dateShortcutsOf(today: LocalDate): List<DateShortcut> = listOf(
    DateShortcut(DateShortcutKind.Today, today),
    DateShortcut(DateShortcutKind.Yesterday, today.minus(1, DateTimeUnit.DAY)),
    DateShortcut(DateShortcutKind.ThisWeek, today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)),
    DateShortcut(DateShortcutKind.ThisMonth, LocalDate(today.year, today.month, 1)),
)

internal fun frequentSectionOf(
    categories: List<SelectableCategory>,
    frequentIds: List<String>,
): List<SelectableCategory> = frequentIds
    .mapNotNull { id -> categories.find { it.categoryId.value == id } }
    .takeIf { it.size >= MIN_FREQUENT_SECTION_SIZE }
    .orEmpty()

internal fun otherSectionOf(
    categories: List<SelectableCategory>,
    frequent: List<SelectableCategory>,
): List<SelectableCategory> = categories.filterNot { it in frequent }

private const val MIN_FREQUENT_SECTION_SIZE: Int = 2
