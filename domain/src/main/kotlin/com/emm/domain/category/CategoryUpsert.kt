package com.emm.domain.category

data class CategoryUpsert(
    val categoryId: String,
    val name: String,
    val icon: String,
    val color: String,
    val categoryType: CategoryType,
)