package com.emm.justchill.hh.domain.categories.crud

import com.emm.justchill.hh.domain.categories.CategoryRepository
import com.emm.justchill.hh.domain.transaction.SyncStatus

class CategoryDeleter(
    private val repository: CategoryRepository,
) {

    suspend fun delete(categoryId: String) {
        repository.updateStatus(categoryId, SyncStatus.PENDING_DELETE)
    }
}