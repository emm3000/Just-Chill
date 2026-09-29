package com.emm.justchill.feature.report

data class CategoryShare(
    val categoryId: String?,
    val name: String,
    val amountFormatted: String,
    val percentage: Int,
    val colorKey: String?,
) {

    val fraction: Float
        get() = percentage / PERCENT_SCALE
}

private const val PERCENT_SCALE: Float = 100f
