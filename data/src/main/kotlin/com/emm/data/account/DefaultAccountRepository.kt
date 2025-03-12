package com.emm.data.account

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.Accounts
import com.emm.data.AccountsQueries
import com.emm.data.EmmDatabaseData
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpsert
import com.emm.domain.shared.UniqueIdProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultAccountRepository(
    private val emmDatabase: EmmDatabaseData,
    private val uniqueIdProvider: UniqueIdProvider,
) : AccountRepository {

    private val aq: AccountsQueries
        get() = emmDatabase.accountsQueries

    override fun retrieve(): Flow<List<Account>> {
        return aq.retrieveAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(List<Accounts>::toDomain)
    }

    override fun findBy(accountId: String): Flow<Account?> {
        return aq.find(accountId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map {
                it?.let(Accounts::toDomain)
            }
    }

    override suspend fun create(
        account: AccountUpsert,
    ) = withContext(Dispatchers.IO) {
        aq.insert(
            accountId = uniqueIdProvider.id,
            name = account.name,
            balance = account.balance,
            description = account.description,
        )
    }

    override fun existDailyAccount(): Flow<Account?> {
        return flowOf(null)
    }

    override suspend fun deleteBy(accountId: String) = withContext(Dispatchers.IO) {
        aq.delete(accountId)
    }

    override suspend fun update(
        accountId: String,
        account: AccountUpsert,
    ) = withContext(Dispatchers.IO) {
        aq.updateValues(
            name = account.name,
            balance = account.balance,
            description = account.description,
            accountId = accountId
        )
    }

    override suspend fun updateAmount(
        accountId: String,
        amount: Double,
    ) = withContext(Dispatchers.IO) {
        aq.updateBalance(amount, accountId)
    }
}