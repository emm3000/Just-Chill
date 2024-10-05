package com.emm.justchill.hh.category.domain

data class CategoryUpsert(
    val name: String,
    val description: String,
    val type: String
)