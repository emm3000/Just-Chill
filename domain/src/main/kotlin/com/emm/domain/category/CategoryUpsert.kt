package com.emm.domain.category

data class CategoryUpsert(
    val categoryId: String,
    val name: String,
    val isSynced: Boolean,
    val updatedAt: Long,
)