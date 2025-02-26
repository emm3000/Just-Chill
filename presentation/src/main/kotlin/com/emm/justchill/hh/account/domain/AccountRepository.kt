package com.emm.justchill.hh.account.domain

import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    fun retrieve(): Flow<List<Account>>

    fun findBy(accountId: String): Flow<Account?>

    suspend fun create(accountId: String, account: AccountUpsert)

    fun existDailyAccount(): Flow<Account?>

    suspend fun deleteBy(accountId: String)

    suspend fun update(accountId: String, account: AccountUpsert)

    suspend fun updateAmount(accountId: String, amount: Double)
}