package com.emm.justchill.hh.transaction

import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId

/**
 * A category as selectable in the transaction/recurring forms. Carries the domain's semantic
 * `iconId`/`colorId` strings; the UI resolves them at render time (CategoryResolve.kt).
 */
data class SelectableCategory(
    val categoryId: CategoryId,
    val name: String,
    val iconId: String,
    val categoryType: CategoryType,
    val colorId: String,
)
