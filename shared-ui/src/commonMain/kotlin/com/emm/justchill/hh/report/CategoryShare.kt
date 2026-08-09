package com.emm.justchill.hh.report

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * UI model for one slice of the income-by-category report.
 *
 * The percentage is computed in the use case (or VM if we cheat in S1).
 * Tint comes from the category's `cat.*` token, mapped at the UI layer.
 */
@Immutable
data class CategoryShare(
    /** Null for the uncategorized bucket, which has no category behind it. */
    val categoryId: String?,
    val name: String,
    val amountFormatted: String,
    val percentage: Int,
    val tint: Color,
)
