package com.emm.justchill.hh.report

data class CategoryShare(
    /** Null for the uncategorized bucket, which has no category behind it. */
    val categoryId: String?,
    val name: String,
    val amountFormatted: String,
    val percentage: Int,
    val colorKey: String?,
)
