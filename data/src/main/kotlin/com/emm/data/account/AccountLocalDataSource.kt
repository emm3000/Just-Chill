package com.emm.data.account

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.Accounts
import com.emm.data.AccountsQueries
import com.emm.data.EmmDatabaseData
import com.emm.data.currentTimeInMillis
import com.emm.domain.account.Account
import com.emm.domain.account.AccountUpsert
import com.emm.domain.shared.UniqueIdProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AccountLocalDataSource(
    private val emmDatabase: EmmDatabaseData,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    private val aq: AccountsQueries
        get() = emmDatabase.accountsQueries

    fun all(): Flow<List<Account>> {
        return aq.all()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(List<Accounts>::toDomain)
    }

    suspend fun find(accountId: String): Account? = withContext(Dispatchers.IO) {
        val accountResult: Accounts? = aq.find(accountId).executeAsOneOrNull()
        return@withContext accountResult?.let {
            Account(
                accountId = it.accountId,
                name = it.name,
                balance = it.balance,
            )
        }
    }

    fun default(): Flow<Account?> = aq
        .all()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map {
            it.firstOrNull()?.let(Accounts::toDomain)
        }

    suspend fun create(account: AccountUpsert) = withContext(Dispatchers.IO) {
        val accountId = uniqueIdProvider.id
        aq.insert(
            accountId = accountId,
            name = account.name,
            balance = account.balance,
            updatedAt = currentTimeInMillis(),
        )
    }

    suspend fun delete(accountId: String) = withContext(Dispatchers.IO) {
        aq.delete(accountId)
    }

    suspend fun update(accountId: String, account: AccountUpsert) = withContext(Dispatchers.IO) {
        aq.update(
            name = account.name,
            balance = account.balance,
            accountId = accountId,
            updatedAt = currentTimeInMillis(),
        )
    }

    suspend fun updateAmount(accountId: String, amount: Double) = withContext(Dispatchers.IO) {
        aq.updateBalance(amount, accountId)
    }
}