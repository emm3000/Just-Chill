package com.emm.domain.category

data class CategoryUpsert(
    val name: String,
    val isSynced: Boolean = false,
)