package com.emm.data.account

import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeApiCall
import com.emm.data.shared.safeDbCall
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpsert
import com.emm.domain.shared.SyncState
import kotlinx.coroutines.flow.Flow

class DefaultAccountRepository(
    private val localDataSource: AccountLocalDataSource,
    private val remoteDataSource: AccountRemoteDataSource,
) : AccountRepository {

    override fun all(): Flow<List<Account>> {
        return localDataSource.all().catchAsDomainException()
    }

    override suspend fun find(accountId: String): Account? = safeDbCall {
        localDataSource.find(accountId)
    }

    override fun default(): Flow<Account?> {
        return localDataSource.default().catchAsDomainException()
    }

    override suspend fun create(account: AccountUpsert): Unit = safeDbCall {
        localDataSource.create(account)
        Unit
    }

    override suspend fun delete(accountId: String): Unit = safeDbCall {
        localDataSource.softDelete(accountId)
        Unit
    }

    override suspend fun update(accountId: String, account: AccountUpsert): Unit = safeDbCall {
        localDataSource.update(accountId, account)
        Unit
    }

    override suspend fun pull() = safeApiCall {
        val networkAccounts: List<NetworkAccount> = remoteDataSource.all()
        networkAccounts.forEach { network ->
            val remoteEntity = network.asEntity()
            val localEntity = localDataSource.findEntity(remoteEntity.accountId)
            when {
                localEntity == null -> localDataSource.insertSynced(remoteEntity)
                localEntity.syncState == SyncState.Pending.name -> Unit // local wins; sync() will push it
                remoteEntity.updatedAt > localEntity.updatedAt -> localDataSource.insertSynced(remoteEntity)
                // else remote is same age or older and local is synced: nothing to do
            }
        }
    }

    override suspend fun sync() = safeApiCall {
        val unSyncedAccounts: List<AccountEntity> = localDataSource.unSynced()
        updateRemote(unSyncedAccounts)
        updateLocal(unSyncedAccounts)
    }

    private suspend fun updateLocal(unSyncedAccounts: List<AccountEntity>) {
        unSyncedAccounts.forEach { localDataSource.markAsSynced(it.accountId) }
    }

    private suspend fun updateRemote(unSyncedAccounts: List<AccountEntity>) {
        val networkAccounts = unSyncedAccounts.map { it.asNetworkModel(userId = "") }
        remoteDataSource.upsert(networkAccounts)
    }
}
