package com.emm.justchill.hh.account.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.Accounts
import com.emm.data.AccountsQueries
import com.emm.data.EmmDatabaseData
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpsert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultAccountRepository(
    private val emmDatabase: EmmDatabaseData,
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
        accountId: String,
        account: AccountUpsert,
    ) = withContext(Dispatchers.IO) {
        aq.insert(
            accountId = accountId,
            name = account.name,
            balance = account.balance,
            initialBalance = account.balance,
            description = account.description,
        )
    }

    override fun existDailyAccount(): Flow<Account?> {
        return aq.existDailyAccount()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { accounts ->
                accounts?.let(Accounts::toDomain)
            }
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
            initialBalance = account.balance,
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