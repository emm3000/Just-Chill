package com.emm.justchill.hh.domain.categories

data class Category(
    val categoryId: String,
    val name: String,
    val type: String,
    val description: String,
    val syncStatus: String,
)
