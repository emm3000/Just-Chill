package com.emm.data.category

data class CategoryEntity(
    val categoryId: String,
    val name: String,
    val icon: String,
    val color: String,
    val categoryType: String,
    val syncState: String,
    val isDefault: Boolean,
    val isDeleted: Boolean,
    val updatedAt: Long,
    val createdAt: Long,
)
