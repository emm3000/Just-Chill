package com.emm.domain.category

data class CategoryUpsert(
    val name: String,
    val description: String,
    val type: String
)