package com.emm.justchill.hh.domain.account

import com.emm.justchill.hh.domain.transaction.SyncStatus
import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    fun retrieve(): Flow<List<Account>>

    fun findBy(accountId: String): Flow<Account?>

    suspend fun create(accountId: String, account: AccountUpsert)

    suspend fun deleteBy(accountId: String)

    suspend fun updateStatus(accountId: String, syncStatus: SyncStatus)

    suspend fun update(accountId: String, account: AccountUpsert)

    suspend fun updateAmount(accountId: String, amount: Double)
}