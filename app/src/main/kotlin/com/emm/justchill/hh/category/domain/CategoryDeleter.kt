package com.emm.justchill.hh.category.domain

import com.emm.justchill.hh.transaction.domain.SyncStatus

class CategoryDeleter(
    private val repository: CategoryRepository,
) {

    suspend fun delete(categoryId: String) {
        repository.updateStatus(categoryId, SyncStatus.PENDING_DELETE)
    }
}