package com.emm.domain.account

import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    fun retrieve(): Flow<List<Account>>

    fun findBy(accountId: String): Flow<Account?>

    fun default(): Flow<Account?>

    suspend fun create(account: AccountUpsert)

    suspend fun deleteBy(accountId: String)
}