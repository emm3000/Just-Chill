package com.emm.data.account

import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpdateRepository
import com.emm.domain.account.AccountUpsert
import kotlinx.coroutines.flow.Flow

class DefaultAccountRepository(
    private val localDataSource: AccountLocalDataSource,
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
}