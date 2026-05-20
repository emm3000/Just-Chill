package com.emm.data.account

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
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

    fun all(): Flow<List<Account>> = aq.all()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { list -> list.asEntity().asExternalModel() }

    suspend fun find(accountId: String): Account? = withContext(Dispatchers.IO) {
        aq.find(accountId).executeAsOneOrNull()?.asEntity()?.asExternalModel()
    }

    fun default(): Flow<Account?> = aq
        .all()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { list ->
            list.firstOrNull()?.asEntity()?.asExternalModel()
        }

    suspend fun create(account: AccountUpsert) = withContext(Dispatchers.IO) {
        aq.insert(
            accountId = account.accountId.value,
            name = account.name,
            type = account.type.name,
            currency = account.currency.name,
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
            type = account.type.name,
            currency = account.currency.name,
            updatedAt = currentTimeInMillis(),
            accountId = accountId,
        )
    }

    suspend fun getBalance(accountId: String): Long = withContext(Dispatchers.IO) {
        emmDatabase.transactionsQueries.getAccountBalance(accountId).executeAsOne()
    }
}
