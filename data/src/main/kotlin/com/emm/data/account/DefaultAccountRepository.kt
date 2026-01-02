package com.emm.data.account

import com.emm.data.Accounts
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
        return localDataSource.all()
    }

    override suspend fun find(accountId: String): Account? {
        return localDataSource.find(accountId)
    }

    override fun default(): Flow<Account?> {
        return localDataSource.default()
    }

    override suspend fun create(account: AccountUpsert) {
        localDataSource.create(account)
    }

    override suspend fun delete(accountId: String) {
        localDataSource.delete(accountId)
    }

    override suspend fun update(accountId: String, account: AccountUpsert) {
        localDataSource.update(accountId, account)
    }

    override suspend fun updateAmount(accountId: String, amount: Double) {
        localDataSource.updateAmount(accountId, amount)
    }

    override suspend fun pull() {
        val all: List<AccountModel> = remoteDataSource.all()
        val accountUpsertList: List<AccountUpsert> = all.map(AccountModel::toAccountUpsert)
        accountUpsertList.forEach {
            localDataSource.create(it)
        }
    }

    override suspend fun sync() {
        val unSyncedAccounts: List<Accounts> = localDataSource.unSynced()
        updateRemote(unSyncedAccounts)
        updateLocal(unSyncedAccounts)
    }

    private suspend fun updateLocal(unSyncedAccounts: List<Accounts>) {
        unSyncedAccounts.forEach { localDataSource.markAsSynced(it.accountId) }
    }

    private suspend fun updateRemote(unSyncedAccounts: List<Accounts>) {
        val accountModels = unSyncedAccounts.map(Accounts::toAccountModel)
        remoteDataSource.upsert(accountModels)
    }
}