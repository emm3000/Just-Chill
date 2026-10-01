package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.presentation.category.SelectableCategory
import com.emm.justchill.core.presentation.format.matchesSearch

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

internal fun List<SelectableCategory>.matching(query: String): List<SelectableCategory> =
    filter { it.name.matchesSearch(query) }

private const val MIN_FREQUENT_SECTION_SIZE: Int = 2
