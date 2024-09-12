package com.emm.justchill.hh.data.account.worker

import androidx.work.ListenableWorker
import com.emm.justchill.hh.data.account.AccountModel
import com.emm.justchill.hh.data.account.AccountRemoteRepository
import com.emm.justchill.hh.data.account.toModel
import com.emm.justchill.hh.data.shared.syncStatus
import com.emm.justchill.hh.domain.account.Account
import com.emm.justchill.hh.domain.account.AccountRepository
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.transaction.SyncStatus
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.firstOrNull

class AccountDeployer(
    private val remoteRepository: AccountRemoteRepository,
    private val repository: AccountRepository,
    private val authRepository: AuthRepository,
) {

    suspend fun deploy(accountId: String): ListenableWorker.Result {

        val account: Account = repository.findBy(accountId).firstOrNull()
            ?: return ListenableWorker.Result.failure()

        val session: UserInfo = authRepository.session()
            ?: return ListenableWorker.Result.retry()

        val syncStatus: SyncStatus = syncStatus(account.syncStatus)
            ?: return ListenableWorker.Result.failure()

        when (syncStatus) {

            SyncStatus.PENDING_INSERT,
            SyncStatus.PENDING_UPDATE -> {
                resolvePendingInsert(account, session, accountId)
            }

            SyncStatus.PENDING_DELETE -> {
                remoteRepository.deleteBy(accountId)
                repository.deleteBy(accountId)
            }

            SyncStatus.SYNCED -> {
                return ListenableWorker.Result.success()
            }
        }
        return ListenableWorker.Result.success()

    }

    private suspend fun resolvePendingInsert(
        account: Account,
        session: UserInfo,
        categoryId: String,
    ) {
        val accountModel: AccountModel = account.toModel(session.id)
        remoteRepository.upsert(accountModel)
        repository.updateStatus(categoryId, SyncStatus.SYNCED)
    }
}