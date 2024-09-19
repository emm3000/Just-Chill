package com.emm.justchill.hh.data.account

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.justchill.Accounts
import com.emm.justchill.AccountsQueries
import com.emm.justchill.EmmDatabase
import com.emm.justchill.hh.data.shared.Syncer
import com.emm.justchill.hh.domain.account.Account
import com.emm.justchill.hh.domain.account.AccountRepository
import com.emm.justchill.hh.domain.account.AccountUpsert
import com.emm.justchill.hh.domain.transaction.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultAccountRepository(
    private val emmDatabase: EmmDatabase,
    private val syncer: Syncer,
): AccountRepository {

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
            syncStatus = SyncStatus.PENDING_INSERT.name
        )
        syncer.sync(accountId)
    }

    override suspend fun deleteBy(accountId: String) = withContext(Dispatchers.IO) {
        aq.delete(accountId)
    }

    override suspend fun updateStatus(
        accountId: String,
        syncStatus: SyncStatus,
    ) = withContext(Dispatchers.IO) {
        aq.updateStatus(syncStatus.name, accountId)
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
            syncStatus = SyncStatus.PENDING_UPDATE.name,
            accountId = accountId
        )
        syncer.sync(accountId)
    }

    override suspend fun updateAmount(
        accountId: String,
        amount: Double,
    ) = withContext(Dispatchers.IO) {
        aq.updateBalance(amount, accountId)
        updateStatus(accountId, SyncStatus.PENDING_UPDATE)
        syncer.sync(accountId)
    }
}