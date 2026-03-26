package com.emm.data.account

import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeApiCall
import com.emm.data.shared.safeDbCall
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpdateRepository
import com.emm.domain.account.AccountUpsert
import kotlinx.coroutines.flow.Flow

class DefaultAccountRepository(
    private val localDataSource: AccountLocalDataSource,
    private val remoteDataSource: AccountRemoteDataSource,
) : AccountRepository, AccountUpdateRepository {

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
        val all: List<NetworkAccount> = remoteDataSource.all()
        val accountUpsertList: List<AccountUpsert> = all.map { networkAccount ->
            AccountUpsert(
                accountId = networkAccount.accountId,
                name = networkAccount.name,
                updatedAt = networkAccount.updatedAt,
                createdAt = networkAccount.createdAt,
            )
        }
        accountUpsertList.forEach {
            localDataSource.create(it)
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
