package com.emm.domain.category

import com.emm.domain.shared.SyncState

data class CategoryUpsert(
    val categoryId: String,
    val name: String,
    val icon: String,
    val color: String,
    val syncState: SyncState,
    val isDeleted: Boolean = false,
    val isDefault: Boolean = false,
    val updatedAt: Long,
    val createdAt: Long,
)