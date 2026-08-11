package com.emm.data.account

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.AccountsQueries
import com.emm.data.EmmDatabaseData
import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.nowMillis
import com.emm.domain.account.Account
import com.emm.domain.account.AccountUpsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class AccountLocalDataSource(private val emmDatabase: EmmDatabaseData, private val clock: Clock) {

    private val aq: AccountsQueries
        get() = emmDatabase.accountsQueries

    fun all(): Flow<List<Account>> = aq.all()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.asEntity().asExternalModel() }

    suspend fun find(accountId: String): Account? = withContext(ioDispatcher) {
        aq.find(accountId).executeAsOneOrNull()?.asEntity()?.asExternalModel()
    }

    fun default(): Flow<Account?> = aq
        .all()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list ->
            list.firstOrNull()?.asEntity()?.asExternalModel()
        }

    suspend fun create(account: AccountUpsert) = withContext(ioDispatcher) {
        aq.insert(
            accountId = account.accountId.value,
            name = account.name,
            type = account.type.name,
            currency = "PEN",
            updatedAt = account.updatedAt,
            createdAt = account.createdAt,
        )
    }

    suspend fun softDelete(accountId: String) = withContext(ioDispatcher) {
        val now = clock.nowMillis()
        aq.softDelete(deletedAt = now, updatedAt = now, accountId = accountId)
    }

    suspend fun update(accountId: String, account: AccountUpsert) = withContext(ioDispatcher) {
        aq.update(
            name = account.name,
            type = account.type.name,
            updatedAt = clock.nowMillis(),
            accountId = accountId,
        )
    }

    suspend fun getBalance(accountId: String): Long = withContext(ioDispatcher) {
        emmDatabase.transactionsQueries.getAccountBalance(accountId).executeAsOne()
    }
}
