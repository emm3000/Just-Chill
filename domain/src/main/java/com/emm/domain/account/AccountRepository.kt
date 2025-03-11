package com.emm.domain.account

import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    fun retrieve(): Flow<List<Account>>

    fun findBy(accountId: String): Flow<Account?>

    suspend fun create(account: AccountUpsert)

    fun existDailyAccount(): Flow<Account?>

    suspend fun deleteBy(accountId: String)

    suspend fun update(accountId: String, account: AccountUpsert)

    suspend fun updateAmount(accountId: String, amount: Double)
}