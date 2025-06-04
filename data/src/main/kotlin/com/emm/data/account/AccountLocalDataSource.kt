package com.emm.data.account

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.Accounts
import com.emm.data.AccountsQueries
import com.emm.data.EmmDatabaseData
import com.emm.domain.account.Account
import com.emm.domain.account.AccountUpsert
import com.emm.domain.shared.currentTimeInMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AccountLocalDataSource(private val emmDatabase: EmmDatabaseData) {

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
        aq.insert(
            accountId = account.accountId,
            name = account.name,
            balance = account.balance,
            synced = account.isSynced,
            updatedAt = account.updatedAt,
        )
    }

    suspend fun delete(accountId: String) = withContext(Dispatchers.IO) {
        aq.delete(accountId)
    }

    suspend fun update(accountId: String, account: AccountUpsert) = withContext(Dispatchers.IO) {
        aq.update(
            name = account.name,
            balance = account.balance,
            synced = account.isSynced,
            updatedAt = currentTimeInMillis(),
            accountId = accountId,
        )
    }

    suspend fun updateAmount(accountId: String, amount: Double) = withContext(Dispatchers.IO) {
        aq.updateBalance(
            synced = false,
            balance = amount,
            accountId = accountId,
        )
    }

    suspend fun unSynced(): List<Accounts> = withContext(Dispatchers.IO) {
        aq.selectByStatus(false).executeAsList()
    }
}