package com.emm.data.account

import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeDbCall
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpsert
import com.emm.domain.shared.AccountId
import kotlinx.coroutines.flow.Flow

class DefaultAccountRepository(private val localDataSource: AccountLocalDataSource) : AccountRepository {

    override fun all(): Flow<List<Account>> = localDataSource.all().catchAsDomainException()

    override suspend fun find(accountId: AccountId): Account? = safeDbCall {
        localDataSource.find(accountId.value)
    }

    override fun default(): Flow<Account?> = localDataSource.default().catchAsDomainException()

    override suspend fun create(account: AccountUpsert): Unit = safeDbCall {
        localDataSource.create(account)
        Unit
    }

    override suspend fun delete(accountId: AccountId): Unit = safeDbCall {
        localDataSource.softDelete(accountId.value)
        Unit
    }

    override suspend fun update(accountId: AccountId, account: AccountUpsert): Unit = safeDbCall {
        localDataSource.update(accountId.value, account)
        Unit
    }
}
