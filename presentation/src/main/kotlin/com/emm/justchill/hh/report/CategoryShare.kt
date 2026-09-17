package com.emm.justchill.hh.report

data class CategoryShare(
    val categoryId: String?,
    val name: String,
    val amountFormatted: String,
    val percentage: Int,
    val colorKey: String?,
)
