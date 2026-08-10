package com.emm.justchill.hh.report

/**
 * UI model for one slice of the income-by-category report.
 *
 * The percentage is computed in the use case (or VM if we cheat in S1).
 * [colorKey] is the category's domain color string; the UI maps it to the
 * `cat.*` token at render time via `domainColorToUi`.
 */
data class CategoryShare(
    /** Null for the uncategorized bucket, which has no category behind it. */
    val categoryId: String?,
    val name: String,
    val amountFormatted: String,
    val percentage: Int,
    val colorKey: String?,
)
