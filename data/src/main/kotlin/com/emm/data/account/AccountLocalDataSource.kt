package com.emm.data.account

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.Accounts
import com.emm.data.AccountsQueries
import com.emm.data.EmmDatabaseData
import com.emm.domain.account.Account
import com.emm.domain.account.AccountUpsert
import com.emm.domain.shared.SyncState
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
            syncState = SyncState.Pending.name,
            isDeleted = false,
            updatedAt = account.updatedAt,
            createdAt = account.createdAt,
        )
    }

    suspend fun delete(accountId: String) = withContext(Dispatchers.IO) {
        aq.delete(accountId)
    }

    suspend fun update(accountId: String, account: AccountUpsert) = withContext(Dispatchers.IO) {
        aq.update(
            name = account.name,
            balance = account.balance,
            syncState = SyncState.Pending.name,
            updatedAt = currentTimeInMillis(),
            accountId = accountId,
        )
    }

    suspend fun markAsSynced(accountId: String) = withContext(Dispatchers.IO) {
        aq.markAsSync(SyncState.Synced.name, accountId)
    }

    suspend fun updateAmount(accountId: String, amount: Double) = withContext(Dispatchers.IO) {
        aq.updateBalance(
            syncState = SyncState.Pending.name,
            balance = amount,
            accountId = accountId,
        )
    }

    suspend fun unSynced(): List<Accounts> = withContext(Dispatchers.IO) {
        aq.selectByStatus(SyncState.Pending.name).executeAsList()
    }
}