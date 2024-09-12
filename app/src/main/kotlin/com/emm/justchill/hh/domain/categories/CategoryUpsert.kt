package com.emm.justchill.hh.domain.categories

data class CategoryUpsert(
    val name: String,
    val description: String,
    val type: String
)