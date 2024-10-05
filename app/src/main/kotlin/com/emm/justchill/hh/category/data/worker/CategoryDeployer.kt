package com.emm.justchill.hh.category.data.worker

import androidx.work.ListenableWorker
import com.emm.justchill.hh.category.data.CategoryModel
import com.emm.justchill.hh.category.data.CategorySupabaseRepository
import com.emm.justchill.hh.category.data.toModel
import com.emm.justchill.hh.shared.syncStatus
import com.emm.justchill.hh.auth.domain.AuthRepository
import com.emm.justchill.hh.category.domain.Category
import com.emm.justchill.hh.category.domain.CategoryRepository
import com.emm.justchill.hh.transaction.domain.SyncStatus
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.firstOrNull

class CategoryDeployer(
    private val remoteRepository: CategorySupabaseRepository,
    private val repository: CategoryRepository,
    private val authRepository: AuthRepository,
) {

    suspend fun deploy(categoryId: String): ListenableWorker.Result {

        val category: Category = repository.findBy(categoryId).firstOrNull()
            ?: return ListenableWorker.Result.failure()

        val session: UserInfo = authRepository.session()
            ?: return ListenableWorker.Result.retry()

        val syncStatus: SyncStatus = syncStatus(category.syncStatus)
            ?: return ListenableWorker.Result.failure()

        when (syncStatus) {

            SyncStatus.PENDING_INSERT,
            SyncStatus.PENDING_UPDATE -> {
                resolvePendingInsert(category, session, categoryId)
            }

            SyncStatus.PENDING_DELETE -> {
                remoteRepository.deleteBy(categoryId)
                repository.deleteBy(categoryId)
            }

            SyncStatus.SYNCED -> {
                return ListenableWorker.Result.success()
            }
        }
        return ListenableWorker.Result.success()

    }

    private suspend fun resolvePendingInsert(
        category: Category,
        session: UserInfo,
        categoryId: String,
    ) {
        val categoryModel: CategoryModel = category.toModel(session.id)
        remoteRepository.upsert(categoryModel)
        repository.updateStatus(categoryId, SyncStatus.SYNCED)
    }
}